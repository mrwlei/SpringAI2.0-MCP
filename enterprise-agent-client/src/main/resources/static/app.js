(() => {
    "use strict";

    const STORAGE_KEY = "enterprise-agent-search-history-v1";
    const MAX_HISTORY = 20;

    const elements = {
        form: document.querySelector("#search-form"),
        input: document.querySelector("#question-input"),
        searchButton: document.querySelector("#search-button"),
        clearButton: document.querySelector("#clear-history"),
        historyNav: document.querySelector("#history-nav"),
        historyCount: document.querySelector("#history-count"),
        conversationList: document.querySelector("#conversation-list"),
        emptyState: document.querySelector("#empty-state"),
        sessionSummary: document.querySelector("#session-summary"),
        modelName: document.querySelector("#model-name"),
        systemPrompt: document.querySelector("#system-prompt"),
        systemStatus: document.querySelector("#system-status"),
        toast: document.querySelector("#toast")
    };

    let history = loadHistory();
    let config = {
        model: (history.length > 0 && history[0].model) || "DeepSeek",
        systemPrompt: (history.length > 0 && history[0].systemPrompt) || "正在读取系统提示词…"
    };
    let toastTimer;

    elements.form.addEventListener("submit", handleSearch);
    elements.clearButton.addEventListener("click", clearHistory);
    elements.input.addEventListener("input", resizeInput);
    elements.input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            elements.form.requestSubmit();
        }
    });

    renderAll();
    loadConfiguration();

    async function loadConfiguration() {
        try {
            const response = await fetch("/api/v1/agent/config", {
                headers: {"Accept": "application/json"}
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const payload = await response.json();
            config = {
                model: payload.model || config.model,
                systemPrompt: payload.systemPrompt || config.systemPrompt
            };
            renderSystemInfo();
            setStatus("ready", "就绪");
        } catch (error) {
            renderSystemInfo();
            setStatus("error", "服务未连接");
        }
    }

    async function handleSearch(event) {
        event.preventDefault();
        const question = elements.input.value.trim();
        if (!question) {
            showToast("请输入要查询的问题", true);
            elements.input.focus();
            return;
        }

        setLoading(true);
        const requestTraceId = createId();
        const entry = normalizeEntry({
            id: createId(),
            traceId: requestTraceId,
            createdAt: new Date().toISOString(),
            question,
            answer: "",
            systemPrompt: config.systemPrompt,
            model: config.model,
            llmRounds: [],
            toolInvocations: [],
            streaming: true
        });
        history.unshift(entry);
        renderAll();
        requestAnimationFrame(() => scrollToEntry(entry.id));

        const streamingCard = document.getElementById(cardId(entry.id));
        const answerContent = streamingCard && streamingCard.querySelector(".answer-content");
        const answerTextNode = document.createTextNode("正在等待模型响应…");
        if (answerContent) {
            answerContent.replaceChildren(answerTextNode);
        }

        let receivedDelta = false;
        let streamCompleted = false;
        try {
            const response = await fetch("/api/v1/agent/query/stream", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "text/event-stream",
                    "X-Trace-Id": requestTraceId
                },
                body: JSON.stringify({question})
            });

            if (!response.ok) {
                throw new Error(await readError(response));
            }

            entry.traceId = response.headers.get("X-Trace-Id") || requestTraceId;
            await consumeEventStream(response, (eventName, payload) => {
                if (eventName === "metadata") {
                    entry.model = String(payload.model || entry.model);
                    entry.systemPrompt = String(payload.systemPrompt || entry.systemPrompt);
                    config = {model: entry.model, systemPrompt: entry.systemPrompt};
                    renderSystemInfo();
                    const modelBadge = streamingCard && streamingCard.querySelector(".card-model");
                    if (modelBadge) {
                        modelBadge.textContent = `🧠 ${entry.model || "DeepSeek"}`;
                    }
                    return;
                }

                if (eventName === "delta") {
                    const content = String(payload.content || "");
                    entry.answer += content;
                    if (!receivedDelta) {
                        answerTextNode.data = "";
                        receivedDelta = true;
                    }
                    answerTextNode.appendData(content);
                    return;
                }

                if (eventName === "done") {
                    const result = payload.result && typeof payload.result === "object"
                        ? payload.result
                        : {};
                    const completedEntry = normalizeEntry({
                        ...entry,
                        answer: result.answer === undefined ? entry.answer : result.answer,
                        systemPrompt: result.systemPrompt || entry.systemPrompt,
                        model: result.model || entry.model,
                        llmRounds: result.llmRounds,
                        toolInvocations: result.toolInvocations,
                        streaming: false
                    });
                    Object.assign(entry, completedEntry);
                    streamCompleted = true;
                    return;
                }

                if (eventName === "error") {
                    throw new Error(payload.message || "查询失败，请稍后重试");
                }
            });

            if (!streamCompleted) {
                throw new Error("流式响应意外中断，请稍后重试");
            }

            history = history.slice(0, MAX_HISTORY);
            config = {model: entry.model, systemPrompt: entry.systemPrompt};
            saveHistory();
            renderAll();
            elements.input.value = "";
            resizeInput();
            requestAnimationFrame(() => scrollToEntry(entry.id));
            showToast("查询完成，完整过程已保存到本地");
        } catch (error) {
            history = history.filter((item) => item.id !== entry.id);
            renderAll();
            setStatus("error", "查询失败");
            showToast(error.message || "查询失败，请稍后重试", true);
        } finally {
            setLoading(false);
        }
    }

    async function consumeEventStream(response, eventHandler) {
        if (!response.body) {
            throw new Error("当前浏览器不支持流式响应");
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";

        try {
            while (true) {
                const {value, done} = await reader.read();
                buffer += decoder.decode(value, {stream: !done});
                buffer = buffer.replace(/\r\n/g, "\n");

                let boundary = buffer.indexOf("\n\n");
                while (boundary >= 0) {
                    const eventBlock = buffer.slice(0, boundary);
                    buffer = buffer.slice(boundary + 2);
                    dispatchEventBlock(eventBlock, eventHandler);
                    boundary = buffer.indexOf("\n\n");
                }

                if (done) {
                    break;
                }
            }

            if (buffer.trim()) {
                dispatchEventBlock(buffer, eventHandler);
            }
        } catch (error) {
            await reader.cancel().catch(() => {});
            throw error;
        } finally {
            reader.releaseLock();
        }
    }

    function dispatchEventBlock(eventBlock, eventHandler) {
        let eventName = "message";
        const dataLines = [];

        String(eventBlock || "").split("\n").forEach((line) => {
            if (!line || line.startsWith(":")) {
                return;
            }
            if (line.startsWith("event:")) {
                eventName = line.slice(6).trim();
            } else if (line.startsWith("data:")) {
                dataLines.push(line.slice(5).replace(/^ /, ""));
            }
        });

        if (dataLines.length === 0) {
            return;
        }

        let payload;
        try {
            payload = JSON.parse(dataLines.join("\n"));
        } catch (error) {
            throw new Error("服务端返回了无法解析的流式数据");
        }
        eventHandler(eventName, payload);
    }

    function renderAll() {
        renderSystemInfo();
        renderHistoryNavigation();
        renderConversations();
    }

    function renderSystemInfo() {
        elements.modelName.textContent = config.model || "DeepSeek";
        elements.systemPrompt.textContent = config.systemPrompt || "暂未获取 System Prompt";
    }

    function renderHistoryNavigation() {
        elements.historyNav.replaceChildren();
        elements.historyCount.textContent = String(history.length);
        elements.clearButton.disabled = history.length === 0
            || elements.form.classList.contains("is-loading");

        if (history.length === 0) {
            elements.historyNav.append(createElement("p", "sidebar-empty", "暂无搜索历史"));
            return;
        }

        history.forEach((entry, index) => {
            const button = createElement("button", "history-link", `${index + 1}. ${entry.question}`);
            button.type = "button";
            button.title = entry.question;
            button.addEventListener("click", () => scrollToEntry(entry.id));
            elements.historyNav.append(button);
        });
    }

    function renderConversations() {
        elements.conversationList.replaceChildren();
        elements.emptyState.classList.toggle("is-hidden", history.length > 0);
        const savedCount = history.filter((entry) => !entry.streaming).length;
        const hasStreamingEntry = history.some((entry) => entry.streaming);
        elements.sessionSummary.textContent = history.length === 0
            ? "本地保存最近 20 轮问答"
            : hasStreamingEntry
                ? `正在生成 · 已保存 ${savedCount} / ${MAX_HISTORY} 轮问答`
                : `已保存 ${savedCount} / ${MAX_HISTORY} 轮问答`;

        history.forEach((entry) => elements.conversationList.append(createConversationCard(entry)));
    }

    function createConversationCard(entry) {
        const card = createElement("article", "conversation-card");
        card.id = cardId(entry.id);
        card.classList.toggle("is-streaming", entry.streaming);

        const header = createElement("header", "card-header");
        const model = createElement("span", "card-model", `🧠 ${entry.model || "DeepSeek"}`);
        const time = createElement("time", "", formatDate(entry.createdAt));
        time.dateTime = entry.createdAt;
        header.append(model, time);

        const question = createMessageBlock("👤 用户问题", entry.question, "question-block");
        const answer = entry.streaming
            ? createStreamingAnswerBlock(entry.answer)
            : createAnswerBlock("📝 最终回答", entry.answer || "模型未返回文本结果");

        const traceSummary = createElement("div", "trace-summary");
        if (entry.streaming) {
            traceSummary.append(
                createElement("span", "trace-chip", "实时生成中"),
                createElement("span", "trace-chip", `Trace ${shortTrace(entry.traceId)}`)
            );
            card.append(header, question, traceSummary, createStreamingStatus(), answer);
            return card;
        }

        traceSummary.append(
            createElement("span", "trace-chip", `模型 ${entry.llmRounds.length} 轮`),
            createElement("span", "trace-chip", `工具 ${entry.toolInvocations.length} 次`),
            createElement("span", "trace-chip", `Trace ${shortTrace(entry.traceId)}`)
        );

        card.append(header, question, traceSummary, createTracePanel(entry), answer);
        return card;
    }

    function createStreamingStatus() {
        const panel = createElement("section", "execution-trace streaming-trace");
        panel.append(createElement("div", "execution-status is-streaming", "⏳ 模型正在生成回答…"));
        return panel;
    }

    function createStreamingAnswerBlock(text) {
        const block = createElement("section", "message-block answer-block");
        const content = createElement(
            "div",
            "message-text answer-content is-streaming",
            text || "正在等待模型响应…"
        );
        block.append(createElement("p", "message-label", "📝 实时回答"), content);
        return block;
    }

    function createMessageBlock(label, text, extraClass) {
        const block = createElement("section", `message-block ${extraClass}`);
        block.append(
            createElement("p", "message-label", label),
            createElement("p", "message-text", text)
        );
        return block;
    }

    function createAnswerBlock(label, text) {
        const block = createElement("section", "message-block answer-block");
        const content = createElement("div", "message-text answer-content");
        renderAnswerContent(content, text);
        block.append(createElement("p", "message-label", label), content);
        return block;
    }

    function renderAnswerContent(container, text) {
        const lines = String(text || "").replace(/\r\n?/g, "\n").split("\n");
        let proseLines = [];
        let index = 0;

        const flushProse = () => {
            appendProse(container, proseLines);
            proseLines = [];
        };

        while (index < lines.length) {
            if (index + 1 < lines.length && isMarkdownTable(lines[index], lines[index + 1])) {
                flushProse();
                const headerCells = splitMarkdownRow(lines[index]);
                const alignmentCells = splitMarkdownRow(lines[index + 1]);
                const bodyRows = [];
                index += 2;

                while (index < lines.length && isMarkdownTableRow(lines[index])) {
                    bodyRows.push(splitMarkdownRow(lines[index]));
                    index += 1;
                }

                container.append(createAnswerTable(headerCells, alignmentCells, bodyRows));
                continue;
            }

            proseLines.push(lines[index]);
            index += 1;
        }

        flushProse();
    }

    function appendProse(container, lines) {
        let paragraphLines = [];

        const flushParagraph = () => {
            if (paragraphLines.length === 0) {
                return;
            }
            const paragraph = createElement("p", "answer-paragraph");
            paragraphLines.forEach((line, index) => {
                if (index > 0) {
                    paragraph.append(document.createElement("br"));
                }
                appendInlineMarkdown(paragraph, line);
            });
            container.append(paragraph);
            paragraphLines = [];
        };

        lines.forEach((line) => {
            if (line.trim() === "") {
                flushParagraph();
            } else {
                paragraphLines.push(line);
            }
        });
        flushParagraph();
    }

    function createAnswerTable(headerCells, alignmentCells, bodyRows) {
        const wrapper = createElement("div", "answer-table-scroll");
        const table = createElement("table", "answer-table");
        const caption = createElement("caption", "sr-only", "查询结果");
        const head = document.createElement("thead");
        const headRow = document.createElement("tr");
        const body = document.createElement("tbody");

        headerCells.forEach((cell, index) => {
            const header = document.createElement("th");
            header.scope = "col";
            applyTableAlignment(header, alignmentCells[index]);
            appendInlineMarkdown(header, cell);
            headRow.append(header);
        });

        bodyRows.forEach((row) => {
            const tableRow = document.createElement("tr");
            headerCells.forEach((unused, index) => {
                const cell = document.createElement("td");
                applyTableAlignment(cell, alignmentCells[index]);
                appendInlineMarkdown(cell, row[index] || "");
                tableRow.append(cell);
            });
            body.append(tableRow);
        });

        head.append(headRow);
        table.append(caption, head, body);
        wrapper.append(table);
        return wrapper;
    }

    function isMarkdownTable(headerLine, delimiterLine) {
        const headerCells = splitMarkdownRow(headerLine);
        const delimiterCells = splitMarkdownRow(delimiterLine);
        return headerCells.length >= 2
            && headerCells.length === delimiterCells.length
            && delimiterCells.every((cell) => /^:?-{3,}:?$/.test(cell.replace(/\s/g, "")));
    }

    function isMarkdownTableRow(line) {
        const value = String(line || "").trim();
        return value !== "" && value.includes("|") && splitMarkdownRow(value).length >= 2;
    }

    function splitMarkdownRow(line) {
        let value = String(line || "").trim();
        if (value.startsWith("|")) {
            value = value.slice(1);
        }
        if (value.endsWith("|") && !value.endsWith("\\|")) {
            value = value.slice(0, -1);
        }

        const cells = [];
        let cell = "";
        let escaped = false;
        for (const character of value) {
            if (escaped) {
                cell += character;
                escaped = false;
            } else if (character === "\\") {
                escaped = true;
            } else if (character === "|") {
                cells.push(cell.trim());
                cell = "";
            } else {
                cell += character;
            }
        }
        if (escaped) {
            cell += "\\";
        }
        cells.push(cell.trim());
        return cells;
    }

    function applyTableAlignment(cell, delimiter) {
        const value = String(delimiter || "").replace(/\s/g, "");
        if (value.startsWith(":") && value.endsWith(":")) {
            cell.classList.add("is-center");
        } else if (value.endsWith(":")) {
            cell.classList.add("is-right");
        }
    }

    function appendInlineMarkdown(container, text) {
        const value = String(text || "");
        const tokenPattern = /(\*\*([^*]+)\*\*|__([^_]+)__|`([^`]+)`)/g;
        let cursor = 0;
        let match;

        while ((match = tokenPattern.exec(value)) !== null) {
            container.append(document.createTextNode(value.slice(cursor, match.index)));
            if (match[4] !== undefined) {
                container.append(createElement("code", "inline-code", match[4]));
            } else {
                container.append(createElement("strong", "", match[2] || match[3]));
            }
            cursor = match.index + match[0].length;
        }
        container.append(document.createTextNode(value.slice(cursor)));
    }

    function createTracePanel(entry) {
        const panel = createElement("section", "execution-trace");
        const status = createElement(
            "div",
            "execution-status",
            `✅ 执行完成（共 ${entry.llmRounds.length} 轮模型调用）`
        );
        const content = createElement("div", "trace-steps");
        content.append(createSystemPromptStep(entry));

        if (entry.llmRounds.length === 0) {
            content.append(createElement("p", "trace-empty", "当前记录没有逐轮模型响应数据。"));
        } else {
            const reasoningRounds = entry.llmRounds.filter((round) => round.toolCallRequested);
            reasoningRounds.forEach((round, index) => {
                content.append(createReasoningRound(entry, round, index));
            });

            const finalRounds = entry.llmRounds.filter((round) => !round.toolCallRequested);
            if (finalRounds.length > 0) {
                content.append(createFinalGenerationStep(entry, finalRounds[finalRounds.length - 1]));
            }
        }

        panel.append(status, content);
        return panel;
    }

    function createSystemPromptStep(entry) {
        const step = createElement("section", "trace-step system-prompt-step");
        step.append(
            createElement("h3", "trace-step-title is-system", "⚙️ 1. System Prompt 生成"),
            createElement(
                "p",
                "trace-step-meta",
                `生成时间：${formatDateTime(entry.createdAt)}  |  共 ${(entry.systemPrompt || "").length} 字符`
            ),
            createTraceDetails("📋 查看完整 System Prompt", entry.systemPrompt || "无", "system-prompt-details")
        );
        return step;
    }

    function createReasoningRound(entry, round, reasoningIndex) {
        const step = createElement("section", "trace-step reasoning-step");
        step.append(
            createElement(
                "h3",
                "trace-step-title is-reasoning",
                `🧠 2.${reasoningIndex + 1} 第 ${round.round} 轮 — ${entry.model || "模型"} 模型推理`
            ),
            createTraceDetails(
                `📡 ${entry.model || "模型"} API 响应 JSON`,
                formatJson(round.rawResult),
                "model-response-details"
            )
        );

        const tools = toolsForRound(entry, round, reasoningIndex);
        if (tools.length === 0) {
            step.append(createElement("p", "trace-empty", "本轮没有采集到工具调用。"));
        } else {
            tools.forEach((tool) => step.append(createToolCallBlock(tool)));
        }
        return step;
    }

    function createFinalGenerationStep(entry, round) {
        const step = createElement("section", "trace-step final-generation-step");
        step.append(
            createElement("h3", "trace-step-title is-final", "🎯 3. 最终回答生成"),
            createElement("p", "trace-step-meta", "生成方式：模型根据工具执行结果生成最终回答"),
            createTraceDetails(
                `📡 查看 ${entry.model || "模型"} API 最终响应 JSON`,
                formatJson(round.rawResult),
                "final-response-details"
            )
        );
        return step;
    }

    function toolsForRound(entry, round, reasoningIndex) {
        const hasRoundInformation = entry.toolInvocations.some((tool) => tool.round > 0);
        if (hasRoundInformation) {
            return entry.toolInvocations.filter((tool) => tool.round === round.round);
        }
        return entry.toolInvocations.filter((tool) => tool.sequence === reasoningIndex + 1);
    }

    function createToolCallBlock(tool) {
        const block = createElement("article", "tool-call-block");
        const header = createElement("header", "tool-call-header");
        const identity = createElement("div", "tool-identity");
        identity.append(
            createElement("span", "tool-label", "🔧 执行工具"),
            createElement("strong", "tool-name", tool.toolName)
        );

        const statusClass = tool.success ? "tool-status is-success" : "tool-status is-error";
        header.append(
            identity,
            createElement("span", statusClass, `${tool.success ? "成功" : "失败"} · ${tool.durationMs}ms`)
        );
        block.append(
            header,
            createTraceDetails("调用参数", formatJson(tool.arguments), "tool-params"),
            createTraceDetails("执行结果", formatJson(tool.result), "tool-result")
        );
        return block;
    }

    function createTraceDetails(summaryText, content, extraClass) {
        const details = createElement("details", `trace-details ${extraClass}`);
        details.append(
            createElement("summary", "", summaryText),
            createElement("pre", "trace-code", content || "无")
        );
        return details;
    }

    function clearHistory() {
        if (history.length === 0 || !window.confirm("确定清除浏览器中的全部搜索历史吗？")) {
            return;
        }
        history = [];
        localStorage.removeItem(STORAGE_KEY);
        renderAll();
        showToast("搜索历史已清除");
    }

    function loadHistory() {
        try {
            const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
            if (!Array.isArray(stored)) {
                return [];
            }
            return stored.slice(0, MAX_HISTORY).map(normalizeEntry);
        } catch (error) {
            localStorage.removeItem(STORAGE_KEY);
            return [];
        }
    }

    function saveHistory() {
        let snapshot = history.slice(0, MAX_HISTORY);
        while (snapshot.length > 0) {
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
                history = snapshot;
                return;
            } catch (error) {
                snapshot = snapshot.slice(0, -1);
            }
        }
        showToast("浏览器存储空间不足，本轮记录未持久化", true);
    }

    function normalizeEntry(value) {
        const entry = value && typeof value === "object" ? value : {};
        return {
            id: safeId(entry.id || createId()),
            traceId: String(entry.traceId || "unknown"),
            createdAt: validDate(entry.createdAt) ? entry.createdAt : new Date().toISOString(),
            question: String(entry.question || ""),
            answer: String(entry.answer || ""),
            systemPrompt: String(entry.systemPrompt || ""),
            model: String(entry.model || "DeepSeek"),
            streaming: Boolean(entry.streaming),
            llmRounds: Array.isArray(entry.llmRounds) ? entry.llmRounds.map((round, index) => ({
                round: Number(round && round.round) || index + 1,
                toolCallRequested: Boolean(round && round.toolCallRequested),
                rawResult: String((round && round.rawResult) || "")
            })) : [],
            toolInvocations: Array.isArray(entry.toolInvocations) ? entry.toolInvocations.map((tool, index) => ({
                sequence: Number(tool && tool.sequence) || index + 1,
                round: Number(tool && tool.round) || 0,
                toolName: String((tool && tool.toolName) || "unknown_tool"),
                arguments: String((tool && tool.arguments) || ""),
                result: String((tool && tool.result) || ""),
                success: !tool || tool.success !== false,
                durationMs: Number(tool && tool.durationMs) || 0
            })) : []
        };
    }

    function setLoading(loading) {
        elements.form.classList.toggle("is-loading", loading);
        elements.input.disabled = loading;
        elements.searchButton.disabled = loading;
        elements.clearButton.disabled = loading || history.length === 0;
        if (loading) {
            setStatus("busy", "模型思考中…");
        } else if (!elements.systemStatus.classList.contains("is-error")) {
            setStatus("ready", "就绪");
        }
    }

    function setStatus(state, text) {
        elements.systemStatus.classList.toggle("is-busy", state === "busy");
        elements.systemStatus.classList.toggle("is-error", state === "error");
        elements.systemStatus.lastElementChild.textContent = text;
    }

    function showToast(message, error = false) {
        window.clearTimeout(toastTimer);
        elements.toast.textContent = message;
        elements.toast.classList.toggle("is-error", error);
        elements.toast.classList.add("is-visible");
        toastTimer = window.setTimeout(() => elements.toast.classList.remove("is-visible"), 3200);
    }

    function resizeInput() {
        elements.input.style.height = "auto";
        elements.input.style.height = `${Math.min(elements.input.scrollHeight, 150)}px`;
    }

    function scrollToEntry(id) {
        const card = document.getElementById(cardId(id));
        if (card) {
            card.scrollIntoView({behavior: "smooth", block: "start"});
        }
    }

    function createElement(tagName, className = "", text = "") {
        const node = document.createElement(tagName);
        if (className) {
            node.className = className;
        }
        node.textContent = text;
        return node;
    }

    function formatJson(value) {
        if (!value) {
            return "";
        }
        try {
            return JSON.stringify(JSON.parse(value), null, 2);
        } catch (error) {
            return String(value);
        }
    }

    async function readError(response) {
        try {
            const payload = await response.json();
            return payload.message || payload.error || `查询失败（HTTP ${response.status}）`;
        } catch (error) {
            return `查询失败（HTTP ${response.status}）`;
        }
    }

    function formatDate(value) {
        return new Intl.DateTimeFormat("zh-CN", {
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        }).format(new Date(value));
    }

    function formatDateTime(value) {
        return new Intl.DateTimeFormat("zh-CN", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
        }).format(new Date(value));
    }

    function validDate(value) {
        return typeof value === "string" && !Number.isNaN(Date.parse(value));
    }

    function createId() {
        if (window.crypto && typeof window.crypto.randomUUID === "function") {
            return window.crypto.randomUUID();
        }
        return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    }

    function safeId(value) {
        return String(value).replace(/[^A-Za-z0-9_-]/g, "-");
    }

    function cardId(id) {
        return `session-${safeId(id)}`;
    }

    function shortTrace(traceId) {
        const value = String(traceId || "unknown");
        return value.length > 12 ? `${value.slice(0, 12)}…` : value;
    }
})();
