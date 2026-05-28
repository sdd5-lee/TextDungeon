package com.textdungeon.system;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.SoundPool;
import com.example.textdungeon.R;

public class SoundManager {
    private static SoundManager instance;
    private MediaPlayer bgmPlayer;
    private SoundPool soundPool;
    private int sfxButtonId;
    private float sfxVolume = 1.0f; //효과음 볼륨 변수

    private SoundManager(Context context) {
        // BGM 설정
        bgmPlayer = MediaPlayer.create(context, R.raw.bgm_main);
        bgmPlayer.setLooping(true);

        // 효과음 설정
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .build();
        sfxButtonId = soundPool.load(context, R.raw.sfx_button, 1);
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public void playBgm() {
        if (bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    public void pauseBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    //볼륨 변수 적용
    public void playButtonSfx() {
        if (soundPool != null) {
            soundPool.play(sfxButtonId, sfxVolume, sfxVolume, 0, 0, 1f);
        }
    }

    // BGM 볼륨 조절
    public void setBgmVolume(float volume) {
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(volume, volume);
        }
    }

    // 효과음 볼륨 조절
    public void setSfxVolume(float volume) {
        this.sfxVolume = volume;
    }

    public void release() {
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
        }
        instance = null;
    }
}