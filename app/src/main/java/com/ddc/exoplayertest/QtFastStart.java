package com.ddc.exoplayertest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Administrator on 2019/11/15.
 * <p>
 * by author wz
 * <p>
 * com.ddc.exoplayertest
 */
public class QtFastStart {

    public static long min(long a, long b) {
        return a > b ? b : a;
    }

    public static short be16() {

        return 1;
    }

    public static long be32(byte[] arr, int sIndex) {
/*		int value = 0;
		value += (arr[sIndex    ] << 24);
		value += (arr[sIndex + 1] << 16);
		value += (arr[sIndex + 2] <<  8);
		value += (arr[sIndex + 3]      );*/

        long[] longArr = new long[4];            //由于java没有无符号的字节(unsigned char)类型，所以有可能越界，故扩展成int型
        for (int i = 0; i < 4; i++) {
            int tempValue = arr[sIndex + i];
            if (tempValue < 0) {
                tempValue = 256 + tempValue;
            }
            longArr[i] = tempValue;
        }
        long longValue = 0;
        longValue += (longArr[0] << 24);
        longValue += (longArr[1] << 16);
        longValue += (longArr[2] << 8);
        longValue += (longArr[3]);
        return longValue;
    }

    public static long be64(byte[] arr, int sIndex) {
/*		long value = 0;
		value += (arr[sIndex] 	  << 56);
		value += (arr[sIndex + 1] << 48);
		value += (arr[sIndex + 2] << 40);
		value += (arr[sIndex + 3] << 32);
		value += (arr[sIndex + 4] << 24);
		value += (arr[sIndex + 5] << 16);
		value += (arr[sIndex + 6] <<  8);
		value += (arr[sIndex + 7]	   );
*/
        long[] longArr = new long[8];        //由于java没有无符号的字节(unsigned char)类型，所以有可能越界，故扩展成long型
        for (int i = 0; i < 8; i++) {
            int tempValue = arr[sIndex + i];
            if (tempValue < 0) {
                tempValue = 256 + tempValue;
            }
            longArr[i] = tempValue;
        }
        long longValue = 0;                    //左移操作符可能造成值溢出，但是视频文件应该不会这么大，所以理论上会溢出
        longValue += (arr[0] << 56);
        longValue += (arr[1] << 48);
        longValue += (arr[2] << 40);
        longValue += (arr[3] << 32);
        longValue += (arr[4] << 24);
        longValue += (arr[5] << 16);
        longValue += (arr[6] << 8);
        longValue += (arr[7]);
        return longValue;
    }

    public static int beFOURCC(char c4, char c3, char c2, char c1) {
        int fourcc = ((int) (c1) |
                ((int) (c2) << 8) |
                ((int) (c3) << 16) |
                ((int) (c4) << 24));
        return fourcc;
    }

    static int FREE_ATOM = beFOURCC('f', 'r', 'e', 'e');
    static int JUNK_ATOM = beFOURCC('j', 'u', 'n', 'k');
    static int MDAT_ATOM = beFOURCC('m', 'd', 'a', 't');
    static int MOOV_ATOM = beFOURCC('m', 'o', 'o', 'v');
    static int PNOT_ATOM = beFOURCC('p', 'n', 'o', 't');
    static int SKIP_ATOM = beFOURCC('s', 'k', 'i', 'p');
    static int WIDE_ATOM = beFOURCC('w', 'i', 'd', 'e');
    static int PICT_ATOM = beFOURCC('P', 'I', 'C', 'T');
    static int FTYP_ATOM = beFOURCC('f', 't', 'y', 'p');
    static int UUID_ATOM = beFOURCC('u', 'u', 'i', 'd');
    static int CMOV_ATOM = beFOURCC('c', 'm', 'o', 'v');
    static int STCO_ATOM = beFOURCC('s', 't', 'c', 'o');
    static int CO64_ATOM = beFOURCC('c', 'o', '6', '4');

    static int ATOM_PREAMBLE_SIZE = 8;
    static int COPY_BUFFER_SIZE = 33554432;


    public static void qtFastStart(String filePath, String fileDesPath) {
        if (filePath == null || filePath.equals("") || fileDesPath == null || fileDesPath.equals("") || filePath.equals(fileDesPath)) {
            return;
        }

        RandomAccessFile inFile = null;
        RandomAccessFile outFile = null;
        byte[] atom_bytes = new byte[ATOM_PREAMBLE_SIZE];
        ;        //果然出现了越界问题，出现了负数。
        long atom_type = 0;
        long atom_size = 0;
        long atom_offset = 0;
        long last_offset;

/*		unsigned char *moov_atom = NULL;	moov_atom = malloc(moov_atom_size);
		unsigned char *ftyp_atom = NULL;	ftyp_atom = malloc(ftyp_atom_size); */
        byte[] moov_atom;
        byte[] ftyp_atom = null;

        long moov_atom_size;
        long ftyp_atom_size = 0;
        int i, j;                //uint64_t i, j;
        long offset_count;
        long current_offset;
        long start_offset = 0;

        //		unsigned char *copy_buffer = NULL;	copy_buffer = malloc(bytes_to_copy);

        byte[] copy_buffer;
        int bytes_to_copy;

        try {
            inFile = new RandomAccessFile(filePath, "r");

            long currentPointer = inFile.getFilePointer();            //文件指针以字节为单位，read()函数后，文件指针会自动增长相应的字节
            System.out.println("RandomAccessFile文件指针的初始位置:" + inFile.getFilePointer());
            System.out.println("RandomAccessFile文件长度:" + inFile.length());
            System.out.println(" ");
            if (currentPointer != 0) {
                return;
            }
            System.out.println("输出格式：");
            System.out.println("box(atom)类型 :  box起始位置(单位byte):  box的size(单位byte):");
            while (currentPointer < inFile.length()) {
                if (inFile.read(atom_bytes, 0, 8) != 8) {        //读取1个ATOM_PREAMBLE_SIZE大小的数据，并放在数组atom_bytes中
                    break;
                }
                //每个atom中的前8个字节表示这个atom的大小(byte为单位)和类型
                atom_size = be32(atom_bytes, 0);
                atom_type = be32(atom_bytes, 4);

                /* keep ftyp atom */
                if (atom_type == FTYP_ATOM) {
                    ftyp_atom_size = atom_size;
                    ftyp_atom = new byte[(int) ftyp_atom_size];            //long强转成int 会不会   损失数据， 应该也是理论上损失，32位int值很大，应该不会
                    // .c原文件中用malloc分配内存，需要判定是否分配成功，java不需要，下面是death code
/*					if(ftyp_atom == null) {
						System.out.println("could not allocate " + atom_size + " bytes for ftyp_atom!");
						return; 	// Throw Exception
					}
					*/
                    inFile.seek(inFile.getFilePointer() - ATOM_PREAMBLE_SIZE);            //文件指针移至 atom起始处，因为前面读取了ATOM_PREAMBLE_SIZE 大小字节的数据，来获取 atom字节过着
                    if (inFile.read(ftyp_atom) != atom_size || (start_offset = inFile.getFilePointer()) < 0) {
                        throw new Exception("读取ftyp_atom失败 || 文件指针 至 文件起始位置失败!");
                    }

                }
                else {        //其余atom
                    long ret;
                    /* 64-bit special case */
                    if (atom_size == 1) {                            //atom_size = 1 表示 这个atom存在一个largesize域，需要64位进行保存，下面代码跟我查的资料不太对应。但是largesize域一般用不到
                        if (inFile.read(atom_bytes) != atom_size) {
                            break;
                        }
                        atom_size = be64(atom_bytes, 0);
                        long pos = atom_size - ATOM_PREAMBLE_SIZE * 2 + inFile.getFilePointer();
                        inFile.seek(pos);
                        if (pos == inFile.getFilePointer()) {
                            ret = 0;
                        }
                        else {
                            ret = -1;
                        }
                    }
                    else {
                        long pos = atom_size - ATOM_PREAMBLE_SIZE + inFile.getFilePointer();
                        inFile.seek(pos);
                        if (pos == inFile.getFilePointer()) {
                            ret = 0;        //表示seek()函数成功，  模仿c的fseek()函数
                        }
                        else {
                            ret = -1;
                        }
                    }
                    if (ret != 0) {
                        throw new Exception("文件指针 回溯到 atom起始位置失败");        // Throw Exception (关闭流)
                    }
                }
                System.out.println(
                        String.valueOf((char) ((atom_type >> 24) & 255))
                                + String.valueOf((char) ((atom_type >> 16) & 255))
                                + String.valueOf((char) ((atom_type >> 8) & 255))
                                + String.valueOf((char) ((atom_type >> 0) & 255)) + " "
                                + String.valueOf(atom_offset) + " "
                                + String.valueOf(atom_size));
                if ((atom_type != FREE_ATOM) &&
                        (atom_type != JUNK_ATOM) &&
                        (atom_type != MDAT_ATOM) &&
                        (atom_type != MOOV_ATOM) &&
                        (atom_type != PNOT_ATOM) &&
                        (atom_type != SKIP_ATOM) &&
                        (atom_type != WIDE_ATOM) &&
                        (atom_type != PICT_ATOM) &&
                        (atom_type != UUID_ATOM) &&
                        (atom_type != FTYP_ATOM)) {
                    System.out.println("encountered non-QT top-level atom (is this a QuickTime file?");
                    break;
                }
                atom_offset += atom_size;
                /* The atom header is 8 (or 16 bytes), if the atom size (which
                 * includes these 8 or 16 bytes) is less than that, we won't be
                 * able to continue scanning sensibly after this atom, so break. */
                if (atom_size < 8) {
                    break;
                }
                currentPointer = inFile.getFilePointer();
            }


            if (atom_type != MOOV_ATOM) {
                System.out.println("last atom in file was not a moov atom, do not transform");
                //free(ftyp_atom);

                //inFile.close();
                //return;
            }
            /* moov atom was, in fact, the last atom in the chunk; load the whole
             * moov atom */
            long pos = inFile.length() - atom_size;            //	找到moov_atom起始位置，并将文件指针指向该位置
            inFile.seek(pos);

            last_offset = inFile.getFilePointer();            // last_offset表示moov_atom的起始位置，同 pos
            moov_atom_size = atom_size;
            moov_atom = new byte[(int) moov_atom_size];

            if (inFile.read(moov_atom, 0, (int) moov_atom_size) != moov_atom_size) {
                throw new Exception("读取 " + moov_atom_size + " byte 数据到moov_atom失败！");
            }
            /* this utility does not support compressed atoms yet, so disqualify
             * files with compressed QT atoms */
            //moov_atom是一个 container atom，moov_atom的前8个byte表示moov_atom的信息，紧跟着的是moov_atom的子atom.
            if (be32(moov_atom, 12) == CMOV_ATOM) {            //若moov_atom的子atom是cmov_atom，表示压缩，现在这个程序不支持
                System.out.println("this utility does not support compressed moov atoms yet");
                throw new Exception();
            }
            /* close; will be re-opened later*/
            inFile.close();
            inFile = null;

            /* crawl through the moov chunk in search of stco or co64 atoms */
            for (i = 4; i < moov_atom_size - 4; i++) {
                atom_type = be32(moov_atom, i);
                if (atom_type == STCO_ATOM) {
                    System.out.println(" patching stco atom...");
                    atom_size = be32(moov_atom, i - 4);
                    if (i + atom_size - 4 > moov_atom_size) {
                        System.out.println(" bad atom size");
                        throw new Exception("bad atom size!");
                    }
                    offset_count = be32(moov_atom, i + 8);
                    if (i + 12 + offset_count * ((long) (4)) > moov_atom_size) {
                        System.out.println(" bad atom size/element count");
                        new Throwable();
                    }
                    for (j = 0; j < offset_count; j++) {
                        current_offset = be32(moov_atom, i + 12 + j * 4);
                        current_offset += moov_atom_size;
                        moov_atom[i + 12 + j * 4 + 0] = (byte) ((current_offset >> 24) & 0xFF);
                        moov_atom[i + 12 + j * 4 + 1] = (byte) ((current_offset >> 16) & 0xFF);
                        moov_atom[i + 12 + j * 4 + 2] = (byte) ((current_offset >> 8) & 0xFF);
                        moov_atom[i + 12 + j * 4 + 3] = (byte) ((current_offset >> 0) & 0xFF);
                    }
                    i += atom_size - 4;
                }
                else if (atom_type == CO64_ATOM) {
                    System.out.println(" patching co64 atom...");
                    atom_size = be32(moov_atom, i - 4);
                    if (i + atom_size - 4 > moov_atom_size) {
                        System.out.println(" bad atom size");
                    }
                    offset_count = be32(moov_atom, i + 8);
                    if (i + 12 + offset_count * ((long) (4)) > moov_atom_size) {
                        System.out.println(" bad atom size/element count");
                        new Throwable();
                    }
                    for (j = 0; j < offset_count; j++) {
                        current_offset = be64(moov_atom, i + 12 + j * 8);
                        current_offset += moov_atom_size;
                        moov_atom[i + 12 + j * 8 + 0] = (byte) ((current_offset >> 56) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 1] = (byte) ((current_offset >> 48) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 2] = (byte) ((current_offset >> 40) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 3] = (byte) ((current_offset >> 32) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 4] = (byte) ((current_offset >> 24) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 5] = (byte) ((current_offset >> 16) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 6] = (byte) ((current_offset >> 8) & 0xFF);
                        moov_atom[i + 12 + j * 8 + 7] = (byte) ((current_offset >> 0) & 0xFF);
                    }
                    i += atom_size - 4;
                }
            }

            /* re-open the input file and open the output file */
            inFile = new RandomAccessFile(filePath, "r");
            if (inFile.getFilePointer() != 0) {
                throw new Exception();
            }
            if (start_offset > 0) {    /* seek after ftyp atom */
                inFile.seek(start_offset);            //将文件指针从当前位置偏移start_offset位置。
                long seek = inFile.getFilePointer();
                if (seek != start_offset) {
                    throw new Exception();
                }
                last_offset -= start_offset;                //start_offset == 0
            }

            outFile = new RandomAccessFile(fileDesPath, "rw");
	/*		if(outFile.getFilePointer() != 0) {
				new Throwable();
			}*/

            /* dump the same ftyp atom */
            if (ftyp_atom_size > 0) {
                System.out.println(" writing ftyp atom...");
                outFile.write(ftyp_atom);
                currentPointer = outFile.getFilePointer();
				/*if(currentPointer != ftyp_atom_size * ftyp_atom.length) {
					new Throwable();
				}*/
            }

            /* dump the new moov atom */
            System.out.println(" writing moov atom...");
            outFile.write(moov_atom);
            currentPointer = outFile.getFilePointer();
/*			if(currentPointer != moov_atom_size * moov_atom.length) {
				new Throwable();
			}*/

            /* copy the remainder of the inFile, from offset 0 -> last_offset - 1 */
            bytes_to_copy = (int) min(COPY_BUFFER_SIZE, last_offset);
            copy_buffer = new byte[bytes_to_copy];
            System.out.println(" copying rest of file...");
            while (last_offset != 0) {
                bytes_to_copy = (int) min(bytes_to_copy, last_offset);
                inFile.read(copy_buffer, 0, bytes_to_copy);

                outFile.write(copy_buffer, 0, bytes_to_copy);

                last_offset -= bytes_to_copy;
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (inFile != null) {
                    inFile.close();
                }
                if (outFile != null) {
                    outFile.close();
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
/*		long value = 156;
		String str = String.valueOf(value << 56);
		System.out.println(str);

		double valu1e = Long.MAX_VALUE + Long.MIN_VALUE;
		int i = Byte.MAX_VALUE;
		System.out.println(valu1e);*/

        new QtFastStart().qtFastStart("app/src/1-1.mp4", "app/src/321.mp4");

    }

}
