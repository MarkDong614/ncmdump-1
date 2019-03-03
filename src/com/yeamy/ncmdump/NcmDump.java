package com.yeamy.ncmdump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class NcmDump {
	private static final byte[] aes_core_key = { 0x68, 0x7A, 0x48, 0x52, 0x41, 0x6D, 0x73, 0x6F, 0x35, 0x6B, 0x49, 0x6E,
			0x62, 0x61, 0x78, 0x57 };
	private static final byte[] aes_modify_key = { 0x23, 0x31, 0x34, 0x6C, 0x6A, 0x6B, 0x5F, 0x21, 0x5C, 0x5D, 0x26,
			0x30, 0x55, 0x3C, 0x27, 0x28 };

	public static boolean dump(File file, File outPath) {
		try {
			Music music = NcmDump.dumpData(file, outPath);
			if (music == null) {
				return false;
			} else if (music.isMP3()) {
				setID3(music);
			} else {
				music.tmpFile().renameTo(music.file);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void setID3(Music music)
			throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
		ID3v23Tag tag = new ID3v23Tag();
		tag.setAlbum(music.album);
		tag.setTitle(music.musicName);
		tag.setArtist(music.artist());
		tag.setAlbumImage(music.cover, "image/jpg");

		File ftmp = music.tmpFile();
		Mp3File mp3file = new Mp3File(ftmp);
		tag.setEncoder(mp3file.getId3v2Tag().getEncoder());
		mp3file.setId3v2Tag(tag);
		mp3file.save(music.file.toString());

		ftmp.delete();
	}

	private static Music dumpData(File file, File outPath) throws IOException {
		FileInputStream fncm = null;
		FileOutputStream fmp3 = null;
		try {
			fncm = new FileInputStream(file);
			byte[] b = new byte[1024];
			fncm.read(b, 0, 8);
			if (!"CTENFDAM".equals(new String(b, 0, 8))) {
				return null;
			}
			fncm.read(b, 0, 2);
			//
			fncm.read(b, 0, 4);
			int key_len = b2i(b);
			byte[] key_data = new byte[key_len];
			fncm.read(key_data);
			for (int i = 0; i < key_data.length; i++) {
				key_data[i] ^= 0x64;
			}
			// ID3 -------------------------------------------
			fncm.read(b, 0, 4);
			int ulen = b2i(b);

			byte[] modifyData = new byte[ulen];
			fncm.read(modifyData);
			for (int i = 0; i < modifyData.length; i++) {
				modifyData[i] ^= 0x63;
			}
			// offset header
			byte[] tmp = new byte[modifyData.length - 22];
			System.arraycopy(modifyData, 22, tmp, 0, tmp.length);
			byte[] data = Base64.getDecoder().decode(tmp);
			byte[] dedata = aes128_ecb_decrypt(data, aes_modify_key);
			String json = new String(dedata, 6, dedata.length - 6).trim();
			Music music = new Gson().fromJson(json, Music.class);
			// read crc32 check
			// fncm.read(b, 0, 4);
			// ulen = b2i(b);
			// fncm.read(b, 0, 5);
			fncm.skip(9);
			// cover -------------------------------------------
			fncm.read(b, 0, 4);
			int img_len = b2i(b);
			if (img_len > 0) {
				byte[] img_data = new byte[img_len];
				fncm.read(img_data);
				music.cover = img_data;
			}
			// mp3 data -------------------------------------------
			byte[] de_key_data = aes128_ecb_decrypt(key_data, aes_core_key);
			int[] box = build_key_box(de_key_data);
//			print(box);
			byte[] buffer = new byte[0x4000];
			String fname = file.getName().replace(".ncm", "." + music.format);
			music.file = new File(outPath, fname);
			fmp3 = new FileOutputStream(music.tmpFile());
			while (true) {
				int n = fncm.read(buffer);
				if (n < 0) {
					break;
				}
				for (int i = 0; i < n; i++) {
					int j = (i + 1) & 0xff;
					int k = (box[j] + j) & 0xff;
					k = (box[j] + box[k]) & 0xff;
					byte key = (byte) (box[k] & 0xff);
					buffer[i] ^= key;
				}
				fmp3.write(buffer, 0, n);
			}
			fmp3.flush();
			return music;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fncm != null) {
				fncm.close();
			}
			if (fmp3 != null) {
				fmp3.close();
			}
		}
	}

	private static int b2i(byte[] b) {
		int i = 0;
		i |= b[0] & 0xff;
		i |= (b[1] & 0xff) << 8;
		i |= (b[2] & 0xff) << 16;
		i |= (b[3] & 0xff) << 24;
		return i;
	}

	public static byte[] aes128_ecb_decrypt(byte[] src, byte[] key) throws Exception {
		int l = src.length;
		int x = l % 16;
		byte[] content = src;
		if (x != 0) {
			content = new byte[l + 16 - x];
			System.arraycopy(src, 0, content, 0, l);
		}
		SecretKeySpec sks = new SecretKeySpec(key, "AES");// 转换为AES专用密钥
		Cipher cipher = Cipher.getInstance("AES_128/ECB/NoPadding");// 实例化
		cipher.init(Cipher.DECRYPT_MODE, sks);// 使用密钥初始化，设置为解密模式
		return cipher.doFinal(content);// 执行操作
	}

	private static int[] build_key_box(byte[] key) {
		int key_len = key.length - 17 - key[key.length - 1];
		byte[] tmp = new byte[key.length - 17];
		System.arraycopy(key, 17, tmp, 0, tmp.length);
		key = tmp;
		int[] box = new int[256];
		for (int i = 0; i < 256; ++i) {
			box[i] = (byte) i;
		}

		int last_byte = 0;
		int key_offset = 0;

		for (int i = 0; i < 256; ++i) {
			int swap = box[i];
			int c = (swap + last_byte + key[key_offset++]) & 0xff;
			if (key_offset >= key_len) {
				key_offset = 0;
			}
			box[i] = box[c];
			box[c] = swap;
			last_byte = c;
		}
		return box;
	}

//	private static void print(byte[] bts) {
//		StringBuilder sb = new StringBuilder();
//		for (byte c : bts) {
//			sb.append("0x").append(Integer.toHexString(c & 0xff)).append(", ");
//		}
//		System.out.println(sb.toString());
//	}

}
