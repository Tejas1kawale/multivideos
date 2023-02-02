package com.example.multivideos;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheKeyFactory;
import com.google.android.exoplayer2.upstream.cache.ContentMetadata;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;

public class Player extends AppCompatActivity {
    public static  String FILE_NAME;
    String vname;
    String Loading_time;
    long st1,pt1;
    TextView mEditText;
    Handler customerHandler=new Handler();
    LinearLayout container;
    TextView timer1,url_view;
    StyledPlayerView playerView;
    ExoPlayer exoPlayer;
    ProgressBar pg;
    SimpleCache simpleCache;
    long MybufferingTime=0;
    int set=0;
    Uri videoUrl;
    long starttime=0L,timemilli=0L,timeswap=0L,updatetime=0L,min,secs,milliseconds;
    Runnable updateTimeThread=new Runnable() {
        @Override
        public void run() {
            timemilli= SystemClock.uptimeMillis()-starttime;
            updatetime=timeswap+timemilli;
             secs=(int)(updatetime/1000);
             min=secs/60;
            secs%=60;
             milliseconds=(int)(updatetime%1000);
            System.out.println("Checking"+exoPlayer.isPlaying());

            if( !exoPlayer.isPlaying())
            {
                System.out.println("Here111");
                ProgressBar progressBar=findViewById(R.id.progressBar);
                progressBar.setVisibility(View.GONE);
            }
            if(set==0)
            {   pt1= Calendar.getInstance().getTimeInMillis();
               long  diff=st1-pt1;
                System.out.println("DDiff== "+diff);
                Date currentTime = Calendar.getInstance().getTime();
                Loading_time= String.valueOf(currentTime);
                timer1.setText(min+":"+secs+":"+milliseconds);
                Loading_time=Loading_time+" "+min+":"+secs+":"+milliseconds+"\n";
                View view = null;
                save(view);

                try {

                    postJsonData();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return;
            }else
            {
                if(set==1){timer1.setText("Buffering");

                }else {
                    timer1.setText("" + min + ":" + String.format("%2d", secs) + ":" + String.format("%3d", milliseconds));
                }

            }

            customerHandler.postDelayed(this,0);
        }
    };
    public static final String TAG = "TAG";
    ProgressBar spinner;
    boolean cache=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        spinner = findViewById(R.id.progressBar);

        Intent i = getIntent();
        Bundle data = i.getExtras();
        Video v = (Video) data.getSerializable("videoData");

        getSupportActionBar().setTitle(v.getName());
        Log.d(TAG, "onCreate:");

        TextView title = findViewById(R.id.videoTitle);
        title.setText(v.getName());

        vname=v.getName();
         videoUrl = Uri.parse(v.getVideoUrl());
        playerView=findViewById(R.id.exoPlayer);

         timer1=(TextView) findViewById(R.id.timer);
        starttime= SystemClock.uptimeMillis();



       url_view=findViewById(R.id.url);
       url_view.setText(v.getVideoUrl());


        // exo player

        // simple cache
        LeastRecentlyUsedCacheEvictor lru=new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);
        StandaloneDatabaseProvider sdp=new StandaloneDatabaseProvider(getApplicationContext());
        if(simpleCache==null){
            simpleCache=new SimpleCache(new File(getApplicationContext().getCacheDir(),"EXOPlayer"+v.getId()+"_"+v.getName()),lru,sdp);
        }

        DefaultHttpDataSource.Factory dfh=new DefaultHttpDataSource.Factory().
                                          setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory dff=new DefaultDataSource.Factory(getApplicationContext(),dfh);

        CacheDataSource.Factory cdf=new CacheDataSource.Factory().setCache(simpleCache).
                                    setUpstreamDataSourceFactory(dff).
                                     setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        exoPlayer=new ExoPlayer.Builder(this).
                setMediaSourceFactory(new DefaultMediaSourceFactory(
                                     cdf)).build();
        // simple cache

        playerView.setPlayer(exoPlayer);
        MediaItem mediaItem=MediaItem.fromUri(videoUrl);
        ProgressiveMediaSource mediaSource= new ProgressiveMediaSource.Factory(cdf).
                                         createMediaSource(mediaItem);
        exoPlayer.setMediaSource(mediaSource,true);
        exoPlayer.prepare();
        System.out.println("cache= "+isVideoCached(videoUrl));
        exoPlayer.play();

        cache=isVideoCached(videoUrl);
        System.out.println("cache after= "+cache);
        System.out.println("Here");

        customerHandler.postDelayed(updateTimeThread,0);
        mEditText=findViewById(R.id.editText);
        Button save1=findViewById(R.id.save);
        Button load1=findViewById(R.id.load);
        save1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                save(view);
            }
        });
        load1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                load(view);
            }
        });

        exoPlayer.addListener(new com.google.android.exoplayer2.Player.Listener() {
            @Override
            public void onIsLoadingChanged(boolean isLoading) {

                if (isLoading) {
                    MybufferingTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    long bufferedTime = currentTime - MybufferingTime;
                    Log.d("Buffered Time", Long.toString(bufferedTime));
                    MybufferingTime = bufferedTime;
                }

                com.google.android.exoplayer2.Player.Listener.super.onIsLoadingChanged(isLoading);
            }});
        }



    private boolean isVideoCached(Uri uri) {
        CacheDataSource.Factory cdf=new CacheDataSource.Factory();
        CacheKeyFactory ckf=cdf.getCacheKeyFactory();
        String contentKey=ckf.buildCacheKey(new DataSpec(uri));
        ContentMetadata cmt=simpleCache.getContentMetadata(contentKey);
        long contentLength=ContentMetadata.getContentLength(cmt);
        if(contentLength<0){
            // this happens when player has never queried this urL over network
            // or has no info about size of the source
            return false;
        }
        long cachedlength=simpleCache.getCachedBytes(contentKey,0L,contentLength);

        return  contentLength>=cachedlength;
        }

    void writeToFile(String videoName, long bufferingTime, boolean isCached) {
        //File file=new File("F:\\AndroidDev\\test.txt");
        try {
            FileOutputStream fos = openFileOutput("F:\\AndroidDev\\test.txt", MODE_APPEND);
            String data = videoName + "\t" + bufferingTime + "\t" + (isCached ? "Cached" : "Not cached") + "\n";
            fos.write(data.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
     }
        public void save(View v)
    {
        String text=Loading_time;
        FILE_NAME=vname+".txt";
        FileOutputStream fos=null;
        try {
            // fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos = openFileOutput(FILE_NAME, MODE_APPEND);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
            myOutWriter.append(text);
            fos.write(text.getBytes());

            Toast.makeText(this,"Saved to"+getFilesDir()+"/"+FILE_NAME,Toast.LENGTH_LONG).show();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }finally {
            if(fos!=null)
            {
                try{
                    fos.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public  void load(View v)
    {
        FileInputStream fis=null;

        try{
            fis=openFileInput(FILE_NAME);
            InputStreamReader isr=new InputStreamReader(fis);
            BufferedReader br=new BufferedReader(isr);
            StringBuilder sb=new StringBuilder();
            String text;
            while((text=br.readLine())!=null)
            {
                sb.append(text).append("\n");

            }
            mEditText.setText(sb.toString());

        }catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }finally {
            if(fis!=null)
            {
                try{
                    fis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }

        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            exoPlayer.stop();
        }
        return super.onOptionsItemSelected(item);
    }
    private void postJsonData() throws JSONException {
        System.out.println("{json calledddd..........}");
        Date currentTime = Calendar.getInstance().getTime();
        String date1=String.valueOf(currentTime);
        String temp=vname+"       "+date1+"      "+min+":"+secs+":"+milliseconds+"  "+cache;

        String URL="http://172.16.25.173:4000/video/";
        RequestQueue requestQueue= Volley.newRequestQueue(this);
        JSONObject json1 =new JSONObject();
        json1.put("tejas",temp);
        JsonObjectRequest objectRequest=new JsonObjectRequest(Request.Method.POST, URL, json1, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.d(TAG, "response=: "+response);
//                try {


//                    JSONArray videos =response.getJSONArray("videos");
//
//                    com.google.android.exoplayer2.util.Log.d(TAG,"changeResponse"+videos);
//
//                    for(int i = 0; i < videos.length() ; i++)
//                    {
//                        JSONObject video=videos.getJSONObject(i);
//                        Video v = new Video();
//                        v.setId(video.getString("id"));
//                        v.setName(video.getString("name"));
//                        String url="http://172.16.23.20:4000/video/";
//                        url=url.concat(video.getString("id"));
//
//                        com.google.android.exoplayer2.util.Log.d(TAG, "url=: "+url);
//
//                        v.setVideoUrl(url);
//
//                        all_videos.add(v);
//                        adapter.notifyDataSetChanged();
//
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
            }
        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                com.google.android.exoplayer2.util.Log.e(TAG,"OnErrorResponse"+error.getMessage());
            }
        });
        requestQueue.add(objectRequest);
    }



}