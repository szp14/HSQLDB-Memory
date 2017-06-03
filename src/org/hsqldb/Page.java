package org.hsqldb;

/**
 * Created by szp on 2017/5/27.
 */

import java.io.IOException;
import java.util.HashMap;

import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.ArrayUtil;
import org.hsqldb.lib.Collection;
import org.hsqldb.lib.LongLookup;
import org.hsqldb.persist.Cache;
import org.hsqldb.persist.CachedObject;
import org.hsqldb.persist.PersistentStore;
import org.hsqldb.rowio.RowInputInterface;
import org.hsqldb.rowio.RowInputBinaryDecode;
import org.hsqldb.rowio.RowOutputInterface;
import org.hsqldb.rowio.RowOutputBinaryEncode;
import org.hsqldb.persist.RandomAccessInterface;


public class Page implements CachedObject {
    public static final int   storageSize = 8192;
    public static long     lastRowPosStart = storageSize - 8;
    public static RowInputInterface rowIn = null;
    public static RowOutputInterface rowOut = null;

    static final int ROW_NUM_POS            = 32;
    static final int ROW_POS_SET_START_POS  = 36;
    public static final int LAST_POS_POS    = 44;
    public static final int HEADER_SIZE     = 64;

    protected int             dataFileScale;
    int                       position;
    public HashMap            rowData;
    //private HashMap           rowInTab;
    private byte[]            buffer;
    protected int             rowNum;
    protected long            rowPosSetStart;
    protected Database        database;

    int              keepCount;
    volatile boolean isInMemory;
    int              accessCount;
    boolean          isNew;

    public Page(Database db, int dataFileScale, long pos)
    {
        this.database = db;
        this.dataFileScale = dataFileScale;

        if(rowIn == null) rowIn = new RowInputBinaryDecode(database.logger.getCrypto(),
                new byte[storageSize]);
        if(rowOut == null) rowOut = new RowOutputBinaryEncode(database.logger.getCrypto(),
                storageSize, dataFileScale);

        position = ((int)pos * dataFileScale) / storageSize;;
        rowData = new HashMap();
        rowNum = 0;
        rowPosSetStart = position * storageSize + storageSize - 8;
        buffer = new byte[storageSize];
    }

    private int readInt(byte[] buf, int pos) {
        return ((buf[pos] & 0xFF) << 24) +
                ((buf[pos + 1] & 0xFF) << 16) +
                ((buf[pos + 2] & 0xFF) << 8) +
                (buf[pos + 3] & 0xFF);
    }

    private long readLong(byte[] buf, int pos) {
        long a = readInt(buf, pos), b = readInt(buf, pos + 4);
        return (a << 32) + b;
    }

    private void writeInt(byte[] buf, int pos, int key) {
        buf[pos] = (byte)((key >>> 24) & 0xFF);
        buf[pos + 1] = (byte)((key >>> 16) & 0xFF);
        buf[pos + 2] = (byte)((key >>> 8) & 0xFF);
        buf[pos + 3] = (byte)(key & 0xFF);
    }

    private void writeLong(byte[] buf, int pos, long key) {
        writeInt(buf, pos, (int)((key >>> 32) & 0xFFFFFFFF));
        writeInt(buf, pos + 4, (int)(key & 0xFFFFFFFF));
    }

    private void read(byte[] src, int pos, byte[] des, int off, int len) {
        System.arraycopy(src, pos, des, off, len);
    }

    public Page(Database db, int dataFileScale, long pos, RandomAccessInterface dataFile) throws IOException {
        this.database   = db;
        this.dataFileScale = dataFileScale;

        if(rowIn == null) rowIn = new RowInputBinaryDecode(database.logger.getCrypto(),
                new byte[storageSize]);
        if(rowOut == null) rowOut = new RowOutputBinaryEncode(database.logger.getCrypto(),
                storageSize, dataFileScale);

        position        =  ((int)pos * dataFileScale) / storageSize;

        dataFile.seek(position * storageSize);
        buffer = new byte[storageSize];
        dataFile.read(buffer, 0, storageSize);

        rowNum          = readInt(buffer, ROW_NUM_POS);
        rowPosSetStart  = readLong(buffer, ROW_POS_SET_START_POS);

        rowData         = new HashMap();
        //rowInTab        = new HashMap();

//        long posSet = (rowPosSetStart + 8) % storageSize;
//
//        for(int i = 0; i < rowNum; i++)
//        {
//            pos = readLong(buffer, (int)posSet);
//            int posInPage = ((int)pos * dataFileScale) % storageSize;
//            rowInTab.put(pos, posInPage);
//            posSet += 8;
//        }
    }

    public CachedObject getRow(long pos, PersistentStore store)
    {
        CachedObject row = (CachedObject)rowData.get(pos);
        if(row != null) return row;

        //int posInPage = (int)rowInTab.get(pos);
        int posInPage = ((int)pos * dataFileScale) % storageSize;
        int size = readInt(buffer, posInPage);

        rowIn.resetRow(pos, size);
        if(size + 4 >= storageSize || posInPage + size >= storageSize)
        {
            size = size;
        }

        read(buffer, posInPage + 4, rowIn.getBuffer(), 4, size - 4);
        row = store.get(rowIn);
        rowData.put(pos, row);
        row.setInMemory(true);
        //rowInTab.remove(pos);
        return row;
    }

    public void setRow(CachedObject row)
    {
        rowData.put(row.getPos(), row);
    }

//    public void writePage(RandomAccessInterface dataFile) {
//        try {
//            dataFile.ensureLength((position + 1) * storageSize);
//
//            java.util.Collection c =  rowData.values();
//            java.util.Iterator it = c.iterator();
//
//            rowNum = 0;
//            rowPosSetStart = position * storageSize + storageSize - 8;;
//            int posInPage = 0;
//            while(it.hasNext())
//            {
//                Row row = (Row)it.next();
//                if(row == null) continue;
//                if(row.hasChanged()) {
//                    rowOut.reset();
//                    row.write(rowOut);
//
//                    dataFile.seek(row.getPos() * dataFileScale);
//                    dataFile.write(rowOut.getOutputStream().getBuffer(), 0,
//                            rowOut.getOutputStream().size());
//
//                    row.setChanged(false);
//                }
//                rowNum++;
//
//                dataFile.seek(rowPosSetStart);
//                dataFile.writeLong(row.getPos());
//
//                rowPosSetStart -= 8;
//            }
//
//            dataFile.seek(position * storageSize + ROW_NUM_POS);
//            dataFile.writeInt(rowNum);
//            dataFile.seek(position * storageSize + ROW_POS_SET_START_POS);
//            dataFile.writeLong(rowPosSetStart);
//            dataFile.seek(LAST_POS_POS);
//            dataFile.writeLong(lastRowPosStart);
//
//        } catch (IOException t) {
//            HsqlException ex = Error.error(ErrorCode.DATA_FILE_ERROR, t);
//            throw ex;
//        }
//    }

    public void writePage(RandomAccessInterface dataFile)
    {
        try {
            dataFile.ensureLength((position + 1) * storageSize + 1);

            java.util.Collection c =  rowData.values();
            java.util.Iterator it = c.iterator();

            rowNum = 0;
            rowPosSetStart = position * storageSize + storageSize - 8;;
            int posInPage = 0;
            while(it.hasNext())
            {
                Row row = (Row)it.next();
                if(row == null) continue;
                if(row.hasChanged()) {
                    rowOut.reset();
                    row.write(rowOut);

                    posInPage = (int)((row.getPos() * dataFileScale) % storageSize);
                    read(rowOut.getOutputStream().getBuffer(), 0, buffer, posInPage, rowOut.getOutputStream().size());

                    row.setChanged(false);
                }
                rowNum++;
                posInPage = (int)(rowPosSetStart % storageSize);
                writeLong(buffer, posInPage, row.getPos());
                rowPosSetStart -= 8;
            }

            writeInt(buffer, ROW_NUM_POS, rowNum);
            writeLong(buffer, ROW_POS_SET_START_POS, rowPosSetStart);
            dataFile.seek(LAST_POS_POS);
            dataFile.writeLong(lastRowPosStart);
            dataFile.seek(position * storageSize + HEADER_SIZE / 2);
            dataFile.write(buffer, HEADER_SIZE / 2, storageSize - HEADER_SIZE / 2);

        } catch (IOException t) {
            HsqlException ex = Error.error(ErrorCode.DATA_FILE_ERROR, t);
            throw ex;
        }
    }

    boolean isDeleted(Session session, PersistentStore store) {

        Page       page = (Page) store.get(this, false);

        if (page == null) {
            return true;
        }
        return false;
    }

    public void setStorageSize(int size) {}

    public int getStorageSize() {
        return storageSize;
    }

    final public boolean isBlock() {
        return true;
    }

    public boolean isMemory() {
        return true;
    }

    public void updateAccessCount(int count) {accessCount = count;}

    public int getAccessCount() {
        return accessCount;
    }

    public long getPos() {
        return position;
    }

    public long getId() {
        return ((long) database.getDatabaseID() << 40) + position;
    }

    public void setPos(long pos) {
        position = (int)pos;
    }

    public boolean isNew() {
        return hasChanged();
    }

    public boolean hasChanged()
    {
        java.util.Collection c =  rowData.values();
        java.util.Iterator it = c.iterator();
        while(it.hasNext())
        {
            Row row = (Row)it.next();
            if(row == null) continue;
            if(row.hasChanged()) return true;
        }
        return false;
    }

    public boolean isKeepInMemory() {
        return false;
    }

    public boolean keepInMemory(boolean keep) {
        return isInMemory = keep;
    }

    public boolean isInMemory() {
        return isInMemory;
    }

    public void setInMemory(boolean in)
    {
        isInMemory = in;
        java.util.Collection c =  rowData.values();
        java.util.Iterator it = c.iterator();
        while(it.hasNext()) {
            Row row = (Row) it.next();
            if (row == null) continue;
            row.setInMemory(in);
        }
    }

    public void delete(PersistentStore store) {}

    public void restore() {}

    public void destroy() {}

    public int getRealSize(RowOutputInterface out) {
        return 0;
    }

    public int getDefaultCapacity() {
        return 0;
    }

    public void read(RowInputInterface in) {}

    public void write(RowOutputInterface out) {}

    public void write(RowOutputInterface out, LongLookup lookup) {}

    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (obj instanceof Page) {
            return ((Page) obj).database == database
                    && ((Page) obj).position == position;
        }

        return false;
    }

    public int hashCode() {
        return (int) position;
    }
}
