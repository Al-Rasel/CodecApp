package bd.com.myapplication;


import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoderThread {
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoDecoder";
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;

    private boolean eosReceived;

    public boolean init(Surface surface, String filePath) {
        eosReceived = false;
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(filePath);

            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mime);


                    mDecoder.setCallback(new MediaCodec.Callback() {
                        @Override
                        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                            ByteBuffer inputBuffer = codec.getInputBuffer(index);
                            int sampleSize = mExtractor.readSampleData(inputBuffer, 0);

                            if (mExtractor.advance() && sampleSize > 0) {
                                codec.queueInputBuffer(index, 0, sampleSize, mExtractor.getSampleTime(), 0);

                            } else {
                                Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                            }

                        }

                        @Override
                        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull BufferInfo info) {

                            codec.releaseOutputBuffer(index, true);

                        }

                        @Override
                        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                        }

                        @Override
                        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                        }
                    }, new Handler());

                    try {
                        Log.d(TAG, "format : " + format);
                        mDecoder.configure(format, surface, null, 0 /* Decoder */);

                    } catch (IllegalStateException e) {
                        Log.e(TAG, "codec '" + mime + "' failed configuration. " + e);
                        return false;
                    }

                    mDecoder.start();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    public void close() {
        eosReceived = true;
    }
}