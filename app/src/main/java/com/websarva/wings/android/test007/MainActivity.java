package com.websarva.wings.android.test007;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.jtransforms.fft.DoubleFFT_1D;        //https://wendykierp.github.io/JTransforms/apidocs/org/jtransforms/fft/DoubleFFT_1D.html // http://tiro105.hateblo.jp/entry/2015/03/02/141526

public class MainActivity extends AppCompatActivity {
    private ReceiveSound receiveSound = new ReceiveSound();


    // クリエイト関係 オーバーライド      アプリ起動時、画面作図前に動作する。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mainbt_receive_start = findViewById(R.id.mainbt_receive_start);

        MainListener mainListener = new MainListener();                                             // リスナ開始

        mainbt_receive_start.setOnClickListener(mainListener);
    }

    @Override
    public void onDestroy() {    // アプリが終了した場合
        super.onDestroy();
        if (receiveSound.getfRecording()) {
            receiveSound.recStop();                                                                 // バックグラウンドのレコーダーを止める。
        }
    }

    // クリック ～ ビュー関係 オーバーライド
    private class MainListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            int objID = view.getId();

            switch (objID) {
                case R.id.mainbt_receive_start:
                    if (!receiveSound.getfRecording()) {
                        receiveSound.execute(0);
                        setBtTitle("受信中止");
                        break;
                    } else {
                        receiveSound.setfRecordingStopPlz();
                        setBtTitle("受信開始");
                        break;
                    }
                default:
                    String mesg = "認識されないオブジェクトがクリックされました。:" + String.valueOf(objID);
                    Toast.makeText(getApplicationContext(), mesg, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public void setTvStat(String mesg) {
        TextView maintv_mesg_receive_stats = findViewById(R.id.maintv_mesg_receive_stats);
        maintv_mesg_receive_stats.setText(mesg);
    }

    public void setBtTitle(String mesg) {
        Button mainbt_receive_start = findViewById(R.id.mainbt_receive_start);                      // 良い方法を探す。
        mainbt_receive_start.setText(mesg);
    }

    // 受信処理関係 ここ
    private class ReceiveSound extends AsyncTask<Integer, Integer, Integer> {
        // AudioRecord 関係
        private static final int SAMPLE_RATE = 44100;                                               // サンプリングレート
        private final int AUDIO_BUFFERSIZE_IN_BYTE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        private final int AUDIO_BUFFERSIZE_IN_SHORT = AUDIO_BUFFERSIZE_IN_BYTE / 2;

        private static final short THRESHOLD_AMP = 0x00ff;      // = 0x00ff;   // 閾値
        private short[] mRecordBuf = new short[AUDIO_BUFFERSIZE_IN_SHORT];
        private boolean fRecording = false;
        private boolean fRecordingStopPlz = false;
        private boolean fQuiet = false;
        // FFT 関係
        private static final int FFT_P_SIZE = 4096;                                                 // フーリエ変換のポイント数
        private final double dB_BASELINE = Math.pow(2, 15) * FFT_P_SIZE * Math.sqrt(2);             // ベースラインの設定？
        private final double RESOLUTION = ((SAMPLE_RATE / (double) FFT_P_SIZE));                    // 分解能の計算
        private int mFFTSize = AUDIO_BUFFERSIZE_IN_SHORT;
        private double[] mFFTBuffer = new double[mFFTSize];
        private DoubleFFT_1D mFFT = new DoubleFFT_1D(mFFTSize);

        private AudioRecord audioRecord = new AudioRecord(      // レコーダーセット
                MediaRecorder.AudioSource.MIC,                  // 音声のソース
                SAMPLE_RATE,                                    // サンプリングレート
                AudioFormat.CHANNEL_IN_MONO,                    // チャンネル設定 MONOとSTEREOが全デバイスサポートとのこと
                AudioFormat.ENCODING_PCM_16BIT,                 // PCM16で、44kと同様に全デバイスサポートとのこと
                AUDIO_BUFFERSIZE_IN_BYTE);


        public boolean getfRecording() {
            return fRecording;
        }

        public void setfRecordingStopPlz() {
            fRecordingStopPlz = true;
        }

        public void recStop() {
            fRecordingStopPlz = true;
            audioRecord.stop();
            fRecording = false;
        }

        // バックグラウンド周りここから
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // バックグラウンド処理の一番最初に実行される
        }

        // AsyncTaskメイン
        @Override
        protected Integer doInBackground(Integer... arryInt) {
            int AVERAGE_NUM         = 10;
            int averageCount        = 0;
            int frequencyAverage    = 0;
            int frequency;

            if (fRecording || fRecordingStopPlz) {
                return arryInt[0];                              // 必要ならエラーコードとして使う。
            }

            audioRecord.startRecording();                       // レコーディングを開始
            fRecording = true;
            while (!fRecordingStopPlz) {                        // バックグラウンド中にループを抜ける場合。
                audioRecord.read(mRecordBuf, 0, AUDIO_BUFFERSIZE_IN_SHORT);         // 読みだして保存
                for (int i = 0; i < AUDIO_BUFFERSIZE_IN_SHORT; i++) {
                    short s = mRecordBuf[i];
                    if (s > THRESHOLD_AMP) {
                        fQuiet = true;
                        continue;
                    } else {
                        fQuiet = false;
                    }
                }

                frequency = doFFT(mRecordBuf);                  // 周波数測定結果
                frequencyAverage += frequency;
                averageCount++;

                try {
                    Thread.sleep(10);                   // スリープ もっと良いやり方を考える。
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(averageCount >= AVERAGE_NUM) {
                    publishProgress((frequencyAverage/AVERAGE_NUM));                     // AsyncTaskの経過報告を使う
                    averageCount = 0;
                    frequencyAverage = 0;
                }

            }
            audioRecord.stop();                                 // レコーディングを終了
            return arryInt[0];
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {  // publishProgress()が実行されると呼び出される
            super.onProgressUpdate(progress);
            setTvStat(getString(R.string.maintv_mesg_frequency_to) + String.valueOf(progress[0]) + getString(R.string.maintv_mesg_frequency_hz));
        }

        @Override
        protected void onPostExecute(Integer arryInt) {         // doInBackground()からreturnされると呼び出される
            super.onPostExecute(arryInt);
            fRecording = false;
            fRecordingStopPlz = false;
        }

        public int oneShot() {      // 試験用
            audioRecord.startRecording();               // レコーディングを開始
            fRecording = true;
            audioRecord.read(mRecordBuf, 0, AUDIO_BUFFERSIZE_IN_SHORT);       // 読みだして保存
            for (int i = 0; i < AUDIO_BUFFERSIZE_IN_SHORT; i++) {
                short s = mRecordBuf[i];
                if (s > THRESHOLD_AMP) {
                    fQuiet = true;
                    continue;
                } else {
                    fQuiet = false;
                }
            }
            int frequency = doFFT(mRecordBuf);               // 周波数測定結果
            audioRecord.stop();                         // レコーディングを終了
            fRecording = false;
            return frequency;
        }

        // FFT関係
        private int doFFT(short[] data) {                       // MAXの周波数成分を抽出
            for (int i = 0; i < mFFTSize; i++) {
                mFFTBuffer[i] = (double) data[i];
            }
            // FFT 実行
            mFFT.realForward(mFFTBuffer);                       // このメソッドは、実際の変換の要素の半分だけを計算しますとのこと

            // 処理結果の複素数配列から各周波数成分の振幅値を求めピーク分の要素番号を得る
            double maxAmp = 0;
            int index = 0;
            int ampArry[] = new int[(mFFTSize / 2) + 1];    // public void realForward（double [] a）メソッドは実際の半分の値のみ計算するため。
            for (int i = 0; i < (mFFTSize / 2); i++) {
                double a = mFFTBuffer[i * 2];               // 実部
                double j = mFFTBuffer[i * 2 + 1];           // 虚部
                // a+ib の絶対値 √ a^2 + b^2 = r が振幅値
                double r = Math.sqrt(a * a + j * j);
                ampArry[i + 1] = (int) r;                   // 要素分のデータを取得
                if (r > maxAmp) {
                    maxAmp = r;
                    index = i;
                }
            }
            // 要素番号・サンプリングレート・FFT サイズからピーク周波数を求める
            ampArry[0] =  index * SAMPLE_RATE / mFFTSize;
            return ampArry[0];
        }
    }

//                        maintv_mesg_receive_stats.setText("周波数 " + String.valueOf(frequency) + "Hz  :" + String.valueOf(fori));
//                        Toast.makeText(getApplicationContext(), "周波数:" + String.valueOf(frequency) + "Hz  :" + String.valueOf(fori), Toast.LENGTH_LONG).show();


}
