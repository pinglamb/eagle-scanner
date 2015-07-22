package com.marsontech.scsi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by pinglamb on 7/22/15.
 */
public class ScsiWrite10 extends CommandBlockWrapper {
    private static final byte LENGTH = 0x10;
    private static final byte OPCODE = 0x2a;

    private int blockAddress;
    private int transferBytes;
    private int blockSize;
    private short transferBlocks;

    /**
     * Constructs a new write command with the given information.
     *
     * @param blockAddress
     *            The logical block address the write should start.
     * @param transferBytes
     *            The bytes which should be transferred.
     * @param blockSize
     *            The block size of the mass storage device.
     */
    public ScsiWrite10(int blockAddress, int transferBytes, int blockSize) {
        super(transferBytes, Direction.OUT, (byte) 0, LENGTH);
        this.blockAddress = (blockAddress / blockSize);
        this.transferBytes = transferBytes;
        this.blockSize = blockSize;
        short transferBlocks = (short) (transferBytes / blockSize);
        if (transferBytes % blockSize != 0) {
            throw new IllegalArgumentException("transfer bytes is not a multiple of block size");
        }
        this.transferBlocks = transferBlocks;
    }

    @Override
    public void serialize(ByteBuffer buffer) {
        super.serialize(buffer);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(OPCODE);
        buffer.put((byte) 0);
        buffer.putInt(blockAddress);
        buffer.put((byte) 0);
        buffer.putShort(transferBlocks);
    }

    @Override
    public String toString() {
        return "ScsiWrite10 [blockAddress=" + blockAddress + ", transferBytes=" + transferBytes
                + ", blockSize=" + blockSize + ", transferBlocks=" + transferBlocks
                + ", getdCbwDataTransferLength()=" + getdCbwDataTransferLength() + "]";
    }

}

