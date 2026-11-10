public class FakeSound {
    public float gain=.75f,pitch=1;public boolean playing=true;
    public float getMasterVolume(){return gain;}public void setMasterVolume(float gain){if(gain<0||gain>.75f)throw new AssertionError("Audio spike");this.gain=gain;}
    public boolean playing(String s){return playing;}public void setPitch(String s,float p){if(p<.5f||p>1)throw new AssertionError();pitch=p;}
    public void pause(String s){playing=false;}public void play(String s){playing=true;}
}
