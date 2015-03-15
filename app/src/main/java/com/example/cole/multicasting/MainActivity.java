package com.example.cole.multicasting;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class MainActivity extends ActionBarActivity {
    private Context context;
    static final String LOG_TAG = "UdpStream";
    static final String AUDIO_FILE_PATH = "file:///storage/emulated/0/Music/Zeds Dead-Journey Of A Lifetime.mp3";
    static final int AUDIO_PORT = 7481;
    static final int SAMPLE_INTERVAL = 10; // milliseconds
    static final int BUF_SIZE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void sndAudio(View view){
        Thread thrd = new Thread(new Runnable(){
            @Override
            public void run()
            {
                Log.e(LOG_TAG, "start send thread, thread id: "
                        + Thread.currentThread().getId());
                long file_size = 0;
                int bytes_read = 0;
                int bytes_count = 0;
                Uri uri = Uri.parse(AUDIO_FILE_PATH);
                File audio = new File(uri.getPath());
                //File audio = new File(AUDIO_FILE_PATH);
                FileInputStream audio_stream = null;
                file_size = audio.length();
                byte[] buf = new byte[BUF_SIZE];
                try
                {
                    DatagramSocket sock = new DatagramSocket();
                    InetAddress address = InetAddress.getByName("192.168.1.101");
                    audio_stream = new FileInputStream(audio);

                    while(bytes_count < file_size)
                    {
                        bytes_read = audio_stream.read(buf, 0, BUF_SIZE);
                        DatagramPacket pack = new DatagramPacket(buf, bytes_read,
                                address, AUDIO_PORT);
                        sock.send(pack);
                        bytes_count += bytes_read;
                        Log.d(LOG_TAG, "bytes_count : " + bytes_count);
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }
                    String cmd = "END_MUSIC";
                    DatagramPacket pack = new DatagramPacket(cmd.getBytes(), cmd.getBytes().length,
                            address, AUDIO_PORT);
                    sock.send(pack);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getBaseContext(),"File Transfer Complete",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch (InterruptedException ie)
                {
                    Log.e(LOG_TAG, "InterruptedException");
                }
                catch (FileNotFoundException fnfe)
                {
                    Log.e(LOG_TAG, "FileNotFoundException");
                }
                catch (SocketException se)
                {
                    Log.e(LOG_TAG, "SocketException");
                }
                catch (UnknownHostException uhe)
                {
                    Log.e(LOG_TAG, "UnknownHostException");
                }
                catch (IOException ie)
                {
                    Log.e(LOG_TAG, "IOException");
                }
            } // end run
        });
        thrd.start();
    }

    public void rcvAudio(View view) {
        Thread thrd = new Thread(new Runnable() {
            @Override
            public void run()
            {
                Log.e(LOG_TAG, "start recv thread, thread id: "
                        + Thread.currentThread().getId());
                try
                {
                    DatagramSocket sock = new DatagramSocket(AUDIO_PORT);
                    byte[] buf = new byte[BUF_SIZE];
                    File file = File.createTempFile("temp", ".mp3", getExternalCacheDir());
                    Log.d(LOG_TAG, file.getAbsolutePath());
                    file.deleteOnExit();
                    FileOutputStream fos = new FileOutputStream(file);
                   // MediaPlayer mPlayer = new MediaPlayer();

                   while(true)
                    {
                        DatagramPacket pack = new DatagramPacket(buf, BUF_SIZE);
                        sock.receive(pack);
                        Log.d(LOG_TAG, "recv pack: " + pack.getLength());
                        if (pack.getLength() <= 9) {
                            String cmd = new String(pack.getData(), 0,
                                    pack.getLength());
                            if (cmd.equals("END_MUSIC")) {
                               // System.out.println("C:Fin de transmission");
                                break;
                            }
                        }
                        fos.write(pack.getData());
                    }

                    fos.close();
                    MediaPlayer mPlayer = new MediaPlayer();
                    Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                    mPlayer.reset();
                    try {
                        mPlayer.setDataSource(getApplicationContext(), uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        mPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mPlayer.start();
                }
                catch (SocketException se)
                {
                    Log.e(LOG_TAG, "SocketException: " + se.toString());
                }
                catch (IOException ie)
                {
                    Log.e(LOG_TAG, "IOException" + ie.toString());
                }
            } // end run
        });
        thrd.start();
    }
}
