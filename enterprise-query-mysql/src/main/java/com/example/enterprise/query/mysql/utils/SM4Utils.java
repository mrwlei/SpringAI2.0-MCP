package com.example.enterprise.query.mysql.utils;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 分组密码有五种工作体制：
 * 1.电码本模式（Electronic Codebook Book (ECB)）
 * 2.密码分组链接模式（Cipher Block Chaining (CBC)）
 * 3.计算器模式（Counter (CTR)）
 * 4.密码反馈模式（Cipher FeedBack (CFB)）
 * 5.输出反馈模式（Output FeedBack (OFB)）
 * @author 张卫广
 */
@SuppressWarnings("restriction")
public class SM4Utils
{

	private static final String secretKey = "72ApzP9ppkuAylCV";


	public static String encryptDataForECB(String plainText)
	{
		if (plainText == null || plainText.isBlank()){
			return plainText;
		}
		try
		{
			SM4_Context ctx = new SM4_Context();
			ctx.isPadding = true;
			ctx.mode = SM4.SM4_ENCRYPT;

			byte[] keyBytes =secretKey.getBytes();
			SM4 sm4 = new SM4();
			sm4.sm4_setkey_enc(ctx, keyBytes);
			byte[] encrypted = sm4.sm4_crypt_ecb(ctx, plainText.getBytes("GBK"));
			String cipherText = Base64.getEncoder().encodeToString(encrypted);
			if (cipherText != null && cipherText.trim().length() > 0)
			{
				Pattern p = Pattern.compile("\\s*|\t|\r|\n");
				Matcher m = p.matcher(cipherText);
				cipherText = m.replaceAll("");
			}
			return cipherText + "$";
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return plainText;
		}
	}

	public static String decryptDataForECB(String cipherText)
	{
		if (cipherText == null || cipherText.isBlank()){
			return cipherText;
		}

		if( cipherText.lastIndexOf("$") != -1 ){
			cipherText = cipherText.substring(0,cipherText.length()-1);
		}else {
			return cipherText;
		}

		try
		{
			SM4_Context ctx = new SM4_Context();
			ctx.isPadding = true;
			ctx.mode = SM4.SM4_DECRYPT;

			byte[] keyBytes = secretKey.getBytes();

			SM4 sm4 = new SM4();
			sm4.sm4_setkey_dec(ctx, keyBytes);
			byte[] decrypted = sm4.sm4_crypt_ecb(ctx, Base64.getDecoder().decode(cipherText));
			return new String(decrypted, "GBK");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return cipherText;
		}
	}

	public static String encryptDataForCBC(String plainText,String secretKey,String iv )
	{
		try {
			SM4_Context ctx = new SM4_Context();
			ctx.isPadding = true;
			ctx.mode = SM4.SM4_ENCRYPT;

			byte[] keyBytes= secretKey.getBytes();
			byte[] ivBytes = iv.getBytes();

			SM4 sm4 = new SM4();
			sm4.sm4_setkey_enc(ctx, keyBytes);
			byte[] encrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, plainText.getBytes("GBK"));
			String cipherText = Base64.getEncoder().encodeToString(encrypted);
			if (cipherText != null && cipherText.trim().length() > 0)
			{
				Pattern p = Pattern.compile("\\s*|\t|\r|\n");
				Matcher m = p.matcher(cipherText);
				cipherText = m.replaceAll("");
			}
			return cipherText;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static String decryptDataForCBC(String cipherText,String secretKey,String iv )
	{
		try
		{
			SM4_Context ctx = new SM4_Context();
			ctx.isPadding = true;
			ctx.mode = SM4.SM4_DECRYPT;

			byte[] keyBytes = secretKey.getBytes() ;
			byte[] ivBytes = iv.getBytes();

			SM4 sm4 = new SM4();
			sm4.sm4_setkey_dec(ctx, keyBytes);
			byte[] decrypted = sm4.sm4_crypt_cbc(ctx, ivBytes, Base64.getDecoder().decode(cipherText));
			return new String(decrypted, "GBK");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}



	public static void main(String[] args) throws IOException
	{
		String plainText = "13220119820503287X";
		plainText.lastIndexOf("$");
		System.out.println(plainText.lastIndexOf("$"));

		System.out.println("ECB模式");
		String cipherText = SM4Utils.encryptDataForECB("15075665686");
		System.out.println("密文: " + cipherText);
		System.out.println("");
		System.out.println();


		System.out.println(SM4Utils.decryptDataForECB("4yqeH3sr9fPy51bWROnTDfb4AqHp6Qt5GcdAvl9Jeuc=$"));
//
//		plainText = SM4Utils.decryptDataForECB(cipherText,"72ApzP8ppkuAylCV");
//		System.out.println("明文: " + plainText);
//		System.out.println("");

//		System.out.println("CBC模式");
//		cipherText = SM4Utils.encryptDataForCBC(plainText,"72ApzP8ppkuAylCV","UISwD9fW6cFh9SNS");
//		System.out.println("密文: " + cipherText);
//		System.out.println("");
//
//		plainText = SM4Utils.decryptDataForCBC(cipherText,"72ApzP8ppkuAylCV","UISwD9fW6cFh9SNS");
//		System.out.println("明文: " + plainText);
	}
}
