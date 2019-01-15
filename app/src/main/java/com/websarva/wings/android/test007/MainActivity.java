package com.websarva.wings.android.test007;

import android.graphics.Bitmap;
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
    private HeatMapView heatMapView = new HeatMapView();


    // クリエイト関係 オーバーライド      アプリ起動時、画面作図前に動作する。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mainbt_receive_start = findViewById(R.id.mainbt_receive_start);

        MainListener mainListener = new MainListener();                                             // リスナ開始

        mainbt_receive_start.setOnClickListener(mainListener);                                      // ボタンをリスナに登録
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
                        receiveSound.execute(0);                            // 最初だけバックグラウンド処理を開始させる。
                    }

                    if(!receiveSound.getfRecordingswitch()){                // 処理中に押されたら　受信中断させる。
                        setBtTitle("受信中断");
                    } else {
                        setBtTitle("受信開始");
                    }

                    receiveSound.setfRecordingswitch();                     // 処理のON OFFを切り替える
                    break;

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

    public void setHeatMapView(Bitmap bitmap) {
        ImageView mainiv_heat_map = findViewById(R.id.mainiv_heat_map);
        mainiv_heat_map.setImageBitmap(bitmap);
    }

    public void setFreqScale(int min, int mid, int max) {
        TextView FreqScaleMin = findViewById(R.id.FreqScaleMin);
        TextView FreqScaleMid = findViewById(R.id.FreqScaleMid);
        TextView FreqScaleMax = findViewById(R.id.FreqScaleMax);

        if (min >= 5000) {
            FreqScaleMin.setText(String.valueOf(min / 1000) + "kHz");
        } else {
            FreqScaleMin.setText(String.valueOf(min) + "Hz");
        }

        if (mid >= 5000) {
            FreqScaleMid.setText(String.valueOf(mid / 1000) + "kHz");
        } else {
            FreqScaleMid.setText(String.valueOf(mid) + "Hz");
        }

        if (max >= 5000) {
            FreqScaleMax.setText(String.valueOf(max / 1000) + "kHz");
        } else {
            FreqScaleMax.setText(String.valueOf(max) + "Hz");
        }


    }

    // 受信処理関係 ここ
    private class ReceiveSound extends AsyncTask<Integer, Integer, Integer> {
        // AudioRecord 関係
        private static final int SAMPLE_RATE = 44100;                                               // サンプリングレート
        private final int AUDIO_BUFFERSIZE_IN_BYTE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        private final int AUDIO_BUFFERSIZE_IN_SHORT = AUDIO_BUFFERSIZE_IN_BYTE / 2;

        private static final short THRESHOLD_AMP = 0x00Af;      // = 0x00ff;   // 閾値
        private short[] mRecordBuf = new short[AUDIO_BUFFERSIZE_IN_SHORT];
        private boolean fRecording = false;
        private boolean fRecordingStopPlz = false;
        private boolean fRecordingswitch = false;
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

        public  void setfRecordingswitch(){
            if(fRecordingswitch){
                fRecordingswitch = false;
            }else{
                fRecordingswitch = true;
            }
        }

        public boolean getfRecordingswitch(){
            return fRecordingswitch;
        }

        public int getFFTSize() {
            return mFFTSize;
        }

        public int getSampleRate() {
            return SAMPLE_RATE;
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
            int AVERAGE_NUM = 10;
            int averageCount = 0;
            int frequencyAverage = 0;

            if (fRecording || fRecordingStopPlz) {
                return arryInt[0];                              // 必要ならエラーコードとして使う。
            }

            audioRecord.startRecording();                       // レコーディングを開始
            fRecording = true;
            while (!fRecordingStopPlz) {                        // バックグラウンド中にループを抜ける場合。
                if(fRecordingswitch) {
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

                    int frequency[] = doFFT(mRecordBuf);
                    int ampMax = 0;
                    int index = 0;
                    for (int i = 0; i < frequency.length; i++) {
                        if (ampMax < frequency[i]) {     // > or >= ？
                            ampMax = frequency[i];
                            index = i;
                        }
                    }

                    frequencyAverage += (index * SAMPLE_RATE / mFFTSize);     // 周波数測定結果
                    averageCount++;

                    try {
                        Thread.sleep(10);                   // スリープのもっと良いやり方を考える。
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    if (averageCount >= AVERAGE_NUM) {
                        Integer[] tediousDataTransfer = new Integer[frequency.length + 2];
                        for (int j = 0; j < frequency.length; j++) {
                            tediousDataTransfer[j] = frequency[j];
                        }
                        tediousDataTransfer[tediousDataTransfer.length - 2] = frequencyAverage / AVERAGE_NUM;     // ややこしい配列の受け渡し。 最後の２つは最大周波数とその振幅値
                        tediousDataTransfer[tediousDataTransfer.length - 1] = ampMax;
                        publishProgress(tediousDataTransfer);                                                   // AsyncTaskの経過報告を使う。
                        averageCount = 0;
                        frequencyAverage = 0;

                    }
                }else{
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

            }
            audioRecord.stop();                                 // レコーディングを終了

            return arryInt[0];
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {  // publishProgress()が実行されると呼び出される
            super.onProgressUpdate(progress);
            setTvStat(getString(R.string.maintv_mesg_frequency_to) + String.valueOf(progress[progress.length - 2]) + getString(R.string.maintv_mesg_frequency_hz));   // 画面表示へ
            setHeatMapView(heatMapView.makeHeatBitMap(progress));                           // ヒートマップ描画
        }

        @Override
        protected void onPostExecute(Integer arryInt) {         // doInBackground()からreturnされると呼び出される
            super.onPostExecute(arryInt);
            fRecording = false;
            fRecordingStopPlz = false;
        }

        // FFT関係
        private int[] doFFT(short[] data) {                     // MAXの周波数成分を抽出
            for (int i = 0; i < mFFTSize; i++) {
                mFFTBuffer[i] = (double) data[i];
            }
            // FFT 実行
            mFFT.realForward(mFFTBuffer);                       // このメソッドは、実際の変換の要素の半分だけを計算しますとのこと

            // 処理結果の複素数配列から各周波数成分の振幅値を求めピーク分の要素番号を得る
            double maxAmp = 0;
            int index = 0;
            int ampArry[] = new int[(mFFTSize / 2)];  // public void realForward（double [] a）のメソッドは実際の半分の値のみ計算するため。
            for (int i = 0; i < (mFFTSize / 2); i++) {
                double a = mFFTBuffer[i * 2];                   // 実部
                double j = mFFTBuffer[i * 2 + 1];               // 虚部
                // a+ib の絶対値 √ a^2 + b^2 = r が振幅値
                ampArry[i] = (int) Math.sqrt(a * a + j * j);
            }
            return ampArry;
        }
    }

    // ヒートマップ表示関係追加

    public class HeatMapView {
        int heatmapWidth = 300;                                 // 動的取得可能にする。mainv_heat_map.getWidth();　は×？
        int heatmapHeight = 170;
        int mapBuf[] = new int[heatmapWidth * heatmapHeight];
        Bitmap heatMapBmp = Bitmap.createBitmap(heatmapWidth, heatmapHeight, Bitmap.Config.ARGB_8888);

        public Bitmap testmakeBMP() {
            for (int y = 0; y < 100; y++) {
                for (int x = 0; x < 100; x++) {
                    heatMapBmp.setPixel(x, y, 0xffffff00);
                }
            }
            return heatMapBmp;
        }

        public Bitmap makeHeatBitMap(Integer... mapNewData) {
            int i;
            int sampleRate = receiveSound.getSampleRate();
            int fftSize = receiveSound.getFFTSize();
            int[] mmapNewData = new int[mapNewData.length - 2];         // 最後の２// つは最大周波数がで不要なため

            for (i = 0; i < mmapNewData.length; i++) {
                mmapNewData[i] = mapNewData[i];                         // (；ﾟДﾟ)
            }

            int colLineNum;                                             // リファクタリングで変数名再定義！
            int pointPower;
            int pointPowerPercent;
            int pointColor;

            for (i = 0; i < heatmapWidth; i++) {
                colLineNum = colFreq(i, mmapNewData.length);
                pointPower = mmapNewData[colLineNum];
                pointPowerPercent = percentCal(pointPower,0xFFFF);              // db計算した方が良い。
//                pointPowerPercent = percentCal(pointPower,(mapNewData[mapNewData.length -1]));
//                pointPowerPercent = percentCal(pointPower, (mapNewData[mapNewData.length - 1] / 4));

                pointColor = dotColor(pointPowerPercent);
                mapBuf[i] = pointColor;
            }

            heatMapBmp.setPixels(mapBuf, 0, heatmapWidth, 0, 0, heatmapWidth, heatmapHeight);     // 配列をBMP形式にする。

            for (i = ((heatmapWidth * heatmapHeight) - heatmapWidth - 1); i >= 0; i--) {          // 次の表示に向けて、配列を１行分ずらす。
                mapBuf[i + heatmapWidth] = mapBuf[i];
            }

            return heatMapBmp;
        }


        private int colFreq(int newLineNum, int freqMAXNum) {                   // 描画する点に対応する要素数を計算
            int DispMmaxFreq = 20 * 1000;                                       // 16kHz    を描画最大に
            setFreqScale(0, (DispMmaxFreq/2), DispMmaxFreq);             // 周波数メモリ表示
            int num = DispMmaxFreq * receiveSound.getFFTSize() / receiveSound.getSampleRate();
//            int retFreqNum = (freqMAXNum * newLineNum) / heatmapWidth;        // サンプルとれただけ表示する（通信時の可聴域外など）
            int retFreqNum = ((num * newLineNum) / heatmapWidth);
            if (retFreqNum == 0) {
                retFreqNum = 1;             // 周波数 0hz 回避   （本来は出ないはず→デバッグ
            }
            return retFreqNum;
        }

        private int percentCal(int now, int max) {              // その値が最大値を100%にした場合何％を計算
            double num = 0.95;
            if (now >= max) {
                now = max;
            }
            for(int i = 1 ; i <= 100; i++){                     // 対数っぽくグラフを表示させる。
                if(now >= (max * num)){
                    return 100 - i;
                }
                max *= num;
            }
            return 0;
//            return (int) ((now * 100) / max);
        }

        private int dotColor(int Percent) {    // パーセントごとの色を決める
            if (Percent > 100) {
                Percent = 100;
            }
            int PerSht = (((100 - Percent) * 24) / 100);            // 100%を24ビットに変換
            int buf = (0xFF000000 | (0x00FC0000 >>> PerSht));
            return buf;
        }
    }
}
