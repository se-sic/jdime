


package net.sf.freecol.client.gui.sound;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;



public class SoundPlayer {

    private static final Logger logger = Logger.getLogger(SoundPlayer.class.getName());

    public static final int STANDARD_DELAY = 2000;
    private static final int MAXIMUM_FADE_MS = 7000;
    private static final int FADE_UPDATE_MS = 5;
    
    
    private ThreadGroup soundPlayerThreads = new ThreadGroup("soundPlayerThreads");

    
    private boolean soundPaused = false;

    
    private boolean soundStopped = true;

    
    private boolean multipleSounds;
    
    
    private SoundPlayerThread currentSoundPlayerThread;

    
    private boolean defaultPlayContinues;

    
    private final int defaultRepeatMode;

    
    private final int defaultPickMode;

    private Mixer mixer;
    
    private PercentageOption volume;

    
    
    public SoundPlayer(AudioMixerOption mixerOption, PercentageOption volume, boolean multipleSounds, boolean defaultPlayContinues) {
        this(mixerOption, volume, multipleSounds, defaultPlayContinues, Playlist.REPEAT_ALL, Playlist.FORWARDS);
    }



    
    public SoundPlayer(AudioMixerOption mixerOption, PercentageOption volume, boolean multipleSounds, boolean defaultPlayContinues, int defaultRepeatMode, int defaultPickMode) {
        if (mixerOption == null) {
            throw new NullPointerException();
        }
        if (volume == null) {
            throw new NullPointerException();
        }
        
        this.volume = volume;
        this.multipleSounds = multipleSounds;
        this.defaultPlayContinues = defaultPlayContinues;
        this.defaultRepeatMode = defaultRepeatMode;
        this.defaultPickMode = defaultPickMode;
        
        mixerOption.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                mixer = AudioSystem.getMixer(((MixerWrapper) e.getNewValue()).getMixerInfo());
            }
        });
        mixer = AudioSystem.getMixer(mixerOption.getValue().getMixerInfo());
    }



    
    public void play(Playlist playlist) {
        play(playlist, defaultPlayContinues, defaultRepeatMode, defaultPickMode, 0);
    }
    
    
    public void play(Playlist playlist, int delay) {
        play(playlist, defaultPlayContinues, defaultRepeatMode, defaultPickMode, delay);
    }

    
    public void playOnce(Playlist playlist) {
        play(playlist, false, defaultRepeatMode, defaultPickMode, 0);
    }
    
    
    public void playOnce(Playlist playlist, int delay) {
        play(playlist, false, defaultRepeatMode, defaultPickMode, delay);
    }

    
    public void play(Playlist playlist, boolean playContinues, int repeatMode, int pickMode, int delay) {
        if (playlist != null) {
            currentSoundPlayerThread = new SoundPlayerThread(playlist, playContinues, repeatMode, pickMode, delay);
            currentSoundPlayerThread.start();
        } else {
            currentSoundPlayerThread = null;
        }
    }



    
    public void stop() {
        soundStopped = true;
        soundPaused = false;
    }



    
    public boolean isStopped() {
        return soundStopped;
    }



    
    public void pause() {
        soundPaused = true;
    }



    
    public boolean isPaused() {
        return soundPaused;
    }




    
    class SoundPlayerThread extends Thread {

        
        private Playlist playlist;

        
        private boolean playContinues;

        
        private int repeatMode;

        
        private int pickMode;

        
        @SuppressWarnings("unused")
        private boolean repeatSound;

        private int delay;

        
        public SoundPlayerThread(Playlist playlist, boolean playContinues, int repeatMode, int pickMode, int delay) {
            super(soundPlayerThreads, FreeCol.CLIENT_THREAD+"SoundPlayer");

            this.playlist = playlist;
            this.playContinues = playContinues;
            this.repeatMode = repeatMode;
            this.pickMode = pickMode;
            this.delay = delay;
        }


        private boolean shouldStopThread() {
            return !multipleSounds && currentSoundPlayerThread != this; 
        }

        
        public void run() {
            playlist.setRepeatMode(repeatMode);
            playlist.setPickMode(pickMode);

            soundPaused = false;
            soundStopped = false;

            if (delay != 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {}
            }
            
            do {
                playSound(playlist.next());

                
                try { Thread.sleep(222); } catch (Exception e) {break;}
            } while (playContinues && playlist.hasNext()
                    && !soundStopped && !shouldStopThread());
        }

        public void playSound(File file) {
            try {
                AudioInputStream in= AudioSystem.getAudioInputStream(file);
                AudioInputStream din = null;
                if (in != null) {
                    AudioFormat baseFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * (16 / 8),
                            baseFormat.getSampleRate(),
                            baseFormat.isBigEndian());
                    din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    rawplay(decodedFormat, din);
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not play audio.", e);
            }
        }

        private void updateVolume(FloatControl c, int volume) {
            
            
            
            
            
            final float gain = 20*(float)Math.log10(volume / 100d);
            c.setValue(gain);
        }
        
        private void rawplay(AudioFormat targetFormat,  AudioInputStream din) throws IOException, LineUnavailableException {
            byte[] data = new byte[8192];
            SourceDataLine line = getLine(targetFormat);
            if (line != null) {
                line.start();
                
                
                final FloatControl c = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                final PropertyChangeListener pcl = new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        int v = ((Integer) e.getNewValue()).intValue();
                        updateVolume(c, v);
                    }
                };
                volume.addPropertyChangeListener(pcl);
                updateVolume(c, volume.getValue());

                
                int read = 0;
                int written = 0;
                try {
                    while (read != -1 && !soundStopped && !shouldStopThread()) {
                        try {
                            while (soundPaused) {
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException e) {}
                        read = din.read(data, 0, data.length);
                        if (read != -1) {
                            written = line.write(data, 0, read);
                        }
                    }
                } finally {
                    volume.removePropertyChangeListener(pcl);
                }
                
                
                if (!soundStopped) {
                    long ms = System.currentTimeMillis() + FADE_UPDATE_MS;
                    long fadeStop = System.currentTimeMillis() + MAXIMUM_FADE_MS;
                    while (read != -1
                            && !soundStopped 
                            && System.currentTimeMillis() < fadeStop) {
                        read = din.read(data, 0, data.length);
                        if (read != -1) {
                            written = line.write(data, 0, read);
                        }
                        if (System.currentTimeMillis() > ms) {
                            
                            float currentGain = c.getValue();
                            float newGain = currentGain - 1f;
                            if (newGain < c.getMinimum())
                                newGain = c.getMinimum();
                            c.setValue(newGain);
                            ms = System.currentTimeMillis() + FADE_UPDATE_MS;
                        }
                    }
                }
                
                line.drain();
                line.stop();
                line.close();
                din.close();
            }             
        }

        private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
            SourceDataLine sdl = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            sdl = (SourceDataLine) mixer.getLine(info);
            sdl.open(audioFormat);
            return sdl;
        }
    }
}
