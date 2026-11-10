package alpha.ambient;

/** Original, deterministic encounter logic. No client classes, graphics, reflection or asset dependencies. */
public final class AmbientHooksDirector {
    public enum Mode { STALK, LURE, INTERCEPT, PASS_BY, TURN_AROUND, TREE_LINE, NULL_LURK, LOST_FOOTSTEPS, REPEATER }
    public enum Phase { IDLE, CUE, OBSERVE, ADVANCE, RETREAT, AFTERMATH, COOLDOWN }
    public static final class Input {
        public boolean observed,visible,figurePresent,nearAnchor,moving,turned;
        public double distance=100;
    }
    public static final class Action {
        public boolean spawn,remove,step,sideways,relocate,cue;
        public double speed;
        public Phase phase;
    }
    private Mode mode;
    private Phase phase=Phase.IDLE;
    private int age,phaseAge,lookTicks,returns;
    private boolean preview;
    public void begin(Mode mode,boolean preview){this.mode=mode;this.preview=preview;age=0;lookTicks=0;returns=0;set(Phase.CUE);}
    public void reset(){phase=Phase.IDLE;age=0;phaseAge=0;lookTicks=0;returns=0;}
    public Phase phase(){return phase;}
    public Mode mode(){return mode;}
    public boolean active(){return phase!=Phase.IDLE;}
    private void set(Phase next){phase=next;phaseAge=0;lookTicks=0;}
    public Action tick(Input in){
        Action a=new Action();a.phase=phase;
        if(!active())return a;
        age++;phaseAge++;
        if(age>1200&&phase!=Phase.COOLDOWN){a.remove=true;set(Phase.COOLDOWN);}
        if(in.figurePresent&&in.distance<14){a.remove=true;returns=3;set(Phase.AFTERMATH);}
        switch(phase){
            case CUE:
                a.cue=phaseAge==1||phaseAge==23;
                if(mode==Mode.REPEATER){
                    a.cue=phaseAge==1||phaseAge==41||phaseAge==81||phaseAge==121;
                    if(phaseAge>160)set(Phase.AFTERMATH);
                }else if(mode==Mode.LOST_FOOTSTEPS){
                    a.cue=phaseAge%18==0&&in.moving;
                    if((phaseAge>30&&in.turned)||phaseAge>(preview?180:320)){a.spawn=true;set(Phase.OBSERVE);}
                }else if(mode==Mode.LURE){
                    // Fixed source is investigated before a figure appears elsewhere.
                    if((phaseAge>25&&in.nearAnchor)||phaseAge>(preview?160:260)){a.spawn=true;set(Phase.OBSERVE);}
                }else if(phaseAge>(preview?25:60)){a.spawn=true;set(Phase.OBSERVE);}
                break;
            case OBSERVE:
                if(!in.figurePresent&&phaseAge>10){set(Phase.AFTERMATH);break;}
                if(in.observed)lookTicks++;else lookTicks=0;
                if((mode==Mode.TREE_LINE||mode==Mode.NULL_LURK)&&lookTicks>=8){a.remove=true;set(Phase.AFTERMATH);}
                else if(lookTicks>=24){set(Phase.RETREAT);}
                else if(mode==Mode.PASS_BY&&phaseAge>12){set(Phase.RETREAT);}
                else if(phaseAge>(preview?35:70)){
                    if(mode==Mode.TREE_LINE||mode==Mode.NULL_LURK||mode==Mode.LOST_FOOTSTEPS){a.remove=true;set(Phase.AFTERMATH);}else set(Phase.ADVANCE);
                }
                break;
            case ADVANCE:
                if(!in.figurePresent){set(Phase.AFTERMATH);break;}
                if(in.observed){lookTicks++;if(lookTicks>=16)set(Phase.RETREAT);}
                else{
                    lookTicks=0;
                    if(in.distance>17){a.step=true;a.speed=.075;}
                    else set(Phase.RETREAT);
                }
                if(phaseAge>180)set(Phase.RETREAT);
                break;
            case RETREAT:
                if(!in.figurePresent){set(Phase.AFTERMATH);break;}
                a.step=true;a.sideways=true;a.speed=.13;
                if((phaseAge>15&&!in.visible)||phaseAge>45){a.remove=true;set(Phase.AFTERMATH);}
                break;
            case AFTERMATH:
                a.remove=true;
                if(phaseAge==25&&mode!=Mode.REPEATER)a.cue=true;
                if(phaseAge>65){
                    if((returns==0&&(mode==Mode.STALK||mode==Mode.TURN_AROUND))||(mode==Mode.TREE_LINE&&returns<2)){
                        returns++;a.spawn=true;a.relocate=true;a.remove=false;set(Phase.OBSERVE);
                    }else set(Phase.COOLDOWN);
                }
                break;
            case COOLDOWN:
                if(phaseAge>200)reset();
                break;
            default:break;
        }
        a.phase=phase;return a;
    }
}
