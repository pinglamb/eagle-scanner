package com.marsontech.scsi;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by pinglamb on 7/22/15.
 */
public class ScsiDeviceDriver {
    private static final String TAG = ScsiDeviceDriver.class.getSimpleName();
    private static final int TIMEOUT = 21000;

    private UsbDeviceConnection usbDeviceConnection;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    private ByteBuffer outBuffer;
    private byte[] cswBuffer;

    private int blockSize;

    public ScsiDeviceDriver(UsbDeviceConnection usbDeviceConnection, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint, int blockSize) {
        this.usbDeviceConnection = usbDeviceConnection;
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
        this.blockSize = blockSize;

        outBuffer = ByteBuffer.allocate(31);
        cswBuffer = new byte[CommandStatusWrapper.SIZE];
    }

    public void read(long devOffset, ByteBuffer dest) throws IOException {
        long time = System.currentTimeMillis();
        // TODO try to make this more efficient by for example only allocating
        // blockSize and making it global
        ByteBuffer buffer;
        if (dest.remaining() % blockSize != 0) {
            Log.i(TAG, "we have to round up size to next block sector");
            int rounded = blockSize - dest.remaining() % blockSize + dest.remaining();
            buffer = ByteBuffer.allocate(rounded);
            buffer.limit(rounded);
        } else {
            buffer = dest;
        }

        ScsiRead10 read = new ScsiRead10((int) devOffset, buffer.remaining(), blockSize);
        transferCommand(read, buffer);

        if (dest.remaining() % blockSize != 0) {
            System.arraycopy(buffer.array(), 0, dest.array(), dest.position(), dest.remaining());
        }

        dest.position(dest.limit());

        Log.d(TAG, "read time: " + (System.currentTimeMillis() - time));
    }

    public void write(long devOffset, ByteBuffer src) throws IOException {
        long time = System.currentTimeMillis();
        // TODO try to make this more efficient by for example only allocating
        // blockSize and making it global
        ByteBuffer buffer;
        if (src.remaining() % blockSize != 0) {
            Log.i(TAG, "we have to round up size to next block sector");
            int rounded = blockSize - src.remaining() % blockSize + src.remaining();
            buffer = ByteBuffer.allocate(rounded);
            buffer.limit(rounded);
            System.arraycopy(src.array(), src.position(), buffer.array(), 0, src.remaining());
        } else {
            buffer = src;
        }

        ScsiWrite10 write = new ScsiWrite10((int) devOffset, buffer.remaining(), blockSize);
        transferCommand(write, buffer);

        src.position(src.limit());
        Log.d(TAG, "write time: " + (System.currentTimeMillis() - time));
    }

    private boolean transferCommand(CommandBlockWrapper command, ByteBuffer inBuffer) throws IOException {
        byte[] outArray = outBuffer.array();
        outBuffer.clear();
        Arrays.fill(outArray, (byte) 0);

        command.serialize(outBuffer);
        int written = usbDeviceConnection.bulkTransfer(outEndpoint, outArray, outArray.length, TIMEOUT);
        if (written != outArray.length) {
            Log.e(TAG, "Writing all bytes on command " + command + " failed!");
        }

        int transferLength = command.getdCbwDataTransferLength();
        int read = 0;
        if (transferLength > 0) {
            byte[] inArray = inBuffer.array();

            if (command.getDirection() == CommandBlockWrapper.Direction.IN) {
                do {
                    int tmp = usbDeviceConnection.bulkTransfer(inEndpoint, inArray, read + inBuffer.position(), inBuffer.remaining() - read, TIMEOUT);
                    if (tmp == -1) {
                        throw new IOException("reading failed!");
                    }
                    read += tmp;
                } while (read < transferLength);

                if (read != transferLength) {
                    throw new IOException("Unexpected command size (" + read + ") on response to "
                            + command);
                }
            } else {
                written = 0;
                do {
                    int tmp = usbDeviceConnection.bulkTransfer(outEndpoint, inArray, written + inBuffer.position(), inBuffer.remaining() - written, TIMEOUT);
                    if (tmp == -1) {
                        throw new IOException("writing failed!");
                    }
                    written += tmp;
                } while (written < transferLength);

                if (written != transferLength) {
                    throw new IOException("Could not write all bytes: " + command);
                }
            }
        }

        // expecting csw now
        read = usbDeviceConnection.bulkTransfer(inEndpoint, cswBuffer, cswBuffer.length, TIMEOUT);
        if (read != CommandStatusWrapper.SIZE) {
            Log.e(TAG, "Unexpected command size while expecting csw");
        }

        CommandStatusWrapper csw = CommandStatusWrapper.read(ByteBuffer.wrap(cswBuffer));
        if (csw.getbCswStatus() != CommandStatusWrapper.COMMAND_PASSED) {
            Log.e(TAG, "Unsuccessful Csw status: " + csw.getbCswStatus());
        }

        if (csw.getdCswTag() != command.getdCbwTag()) {
            Log.e(TAG, "wrong csw tag!");
        }

        return csw.getbCswStatus() == CommandStatusWrapper.COMMAND_PASSED;
    }
}
