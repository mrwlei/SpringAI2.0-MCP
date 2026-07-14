package com.example.enterprise.agent.model;

public record AgentStreamEvent(
        Type type,
        String content,
        String systemPrompt,
        String model,
        AgentQueryResult result,
        String message) {

    public static AgentStreamEvent metadata(String systemPrompt, String model) {
        return new AgentStreamEvent(Type.METADATA, null, systemPrompt, model, null, null);
    }

    public static AgentStreamEvent delta(String content) {
        return new AgentStreamEvent(Type.DELTA, content, null, null, null, null);
    }

    public static AgentStreamEvent done(AgentQueryResult result) {
        return new AgentStreamEvent(Type.DONE, null, null, null, result, null);
    }

    public static AgentStreamEvent error(String message) {
        return new AgentStreamEvent(Type.ERROR, null, null, null, null, message);
    }

    public enum Type {
        METADATA("metadata"),
        DELTA("delta"),
        DONE("done"),
        ERROR("error");

        private final String eventName;

        Type(String eventName) {
            this.eventName = eventName;
        }

        public String eventName() {
            return eventName;
        }
    }
}
