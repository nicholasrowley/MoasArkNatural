package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.widget.MediaController;

public class MusicController extends MediaController {

    public MusicController(Context c){
        super(c);
    }

    public void hide(){}

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        /*songPosn--;
        if(songPosn&lt;0) songPosn=songs.size()-1;
        playSong();*/
    }

    public void playNext(){
        /*songPosn++;
        if(songPosn&gt;=songs.size()) songPosn=0;
        playSong();*/
    }

}
