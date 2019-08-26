package com.jeesite.modules.transmission.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jeesite.common.collect.ListUtils;
import com.jeesite.common.io.FileUtils;
import com.jeesite.modules.transmission.entity.TempFile;
import com.jeesite.modules.transmission.service.TempFileService;

/**
 * 文件处理器
 * 
 * @author 彭嘉辉
 *
 */
@Component
public class FileHandler {
	/*
	 * 碎片文件大小，1M
	 */
	private static final long BLOCK_SIZE = 1024 * 1024;

	@Autowired
	private TempFileService tempFileService;

	/**
	 * 拆分文件
	 * 
	 * @param targetFile
	 *            需要拆分的文件全路径
	 * @param busType
	 *            业务类型，用于记录这次数据传输是哪个业务，随意定义
	 * @return 碎片文件个数
	 * @throws Exception
	 */
	public List<TempFile> splitFile(String targetFile, String busType) {
		File target = new File(targetFile);
		RandomAccessFile randomAccessFile = null;
		// DataSourceTransactionManager dataSourceTransactionManager = null;
		// TransactionStatus status = null;
		long count = -1L;
		List<TempFile> returnList = null;
		try {
			randomAccessFile = new RandomAccessFile(targetFile, "r");
			long fileSize = randomAccessFile.length();
			// 拆分后的碎片文件个数
			count = fileSize / BLOCK_SIZE;
			// 处理最后多出一小份的情况
			if (fileSize % BLOCK_SIZE != 0) {
				count++;
			}
			// 创建存放碎片文件的目录
			String piceDir = target.getParent() + File.separator + "pice";
			FileUtils.createDirectory(piceDir);
			// 将文件拆分成若干临时碎片文件
			List<TempFile> tempFileList = ListUtils.newArrayList();
			for (long i = 0; i < count; i++) {
				String piceFileName = piceDir + File.separator + target.getName() + "_" + i + ".tmp";
				FileUtils.createFile(piceFileName);
				byte[] b = new byte[(int) BLOCK_SIZE];
				randomAccessFile.seek(i * BLOCK_SIZE);
				int len = randomAccessFile.read(b);
				File piceFile = new File(piceFileName);
				FileUtils.writeByteArrayToFile(piceFile, b, 0, len);
				// 记录临时碎片文件信息
				TempFile tempFile = new TempFile();
				tempFile.setBusType(busType);
				tempFile.setPath(piceFile.getParent());
				tempFile.setFileName(target.getName());
				tempFile.setPiceFileName(piceFile.getName());
				tempFile.setPoint(i);
				tempFileList.add(tempFile);
			}
			if (tempFileList.size() > 0) {
				returnList = tempFileService.insertBatch(tempFileList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				randomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return returnList;
	}

	public boolean mergeFile(String targetFile, byte[] piceFile, long point) {
		RandomAccessFile randomAccessFile = null;
		try {
			FileUtils.createFile(targetFile);
			System.out.println("第" + point + "碎片从" + point * BLOCK_SIZE + "开始写入" + piceFile.length);
			randomAccessFile = new RandomAccessFile(targetFile, "rw");
			randomAccessFile.seek(point * BLOCK_SIZE);
			randomAccessFile.write(piceFile);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				randomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public static void main(String[] args) {
		// File file = new File("D:\\jeesite\\userfiles\\jsonfile\\测试1.json");
		// System.out.println(file.getParent());
		String str = "1566034615581.zip_0.tmp";
		System.out.println(str.substring(str.indexOf("_") + 1, str.lastIndexOf(".")));
	}
}
