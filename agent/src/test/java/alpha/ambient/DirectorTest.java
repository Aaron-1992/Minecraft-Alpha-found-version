package alpha.ambient;

import org.junit.Test;
import static org.junit.Assert.*;

public class DirectorTest {
    static AmbientHooksDirector.Input input(){return new AmbientHooksDirector.Input();}
    static void reach(AmbientHooksDirector d,AmbientHooksDirector.Input in,AmbientHooksDirector.Phase phase){for(int i=0;i<600&&d.phase()!=phase;i++)d.tick(in);assertEquals(phase,d.phase());}
    @Test public void cueThenObservationThenUnwatchedAdvance(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.STALK,true);AmbientHooksDirector.Input in=input();
        assertTrue(d.tick(in).cue);boolean spawn=false;for(int i=0;i<30;i++)spawn|=d.tick(in).spawn;assertTrue(spawn);
        in.figurePresent=true;in.distance=27;reach(d,in,AmbientHooksDirector.Phase.ADVANCE);
        AmbientHooksDirector.Action move=d.tick(in);assertTrue(move.step);assertFalse(move.sideways);assertEquals(.075,move.speed,.0001);
    }
    @Test public void lookingFreezesAdvanceThenForcesRetreat(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.STALK,true);AmbientHooksDirector.Input in=input();
        for(int i=0;i<27;i++)d.tick(in);in.figurePresent=true;in.distance=26;reach(d,in,AmbientHooksDirector.Phase.ADVANCE);
        in.observed=true;for(int i=0;i<10;i++)assertFalse(d.tick(in).step);reach(d,in,AmbientHooksDirector.Phase.RETREAT);
        assertTrue(d.tick(in).sideways);
    }
    @Test public void lureWaitsForInvestigationAndStillHasTimeout(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.LURE,false);AmbientHooksDirector.Input in=input();
        for(int i=0;i<80;i++)assertFalse(d.tick(in).spawn);in.nearAnchor=true;assertTrue(d.tick(in).spawn);
        d.begin(AmbientHooksDirector.Mode.LURE,true);in.nearAnchor=false;boolean spawn=false;for(int i=0;i<165;i++)spawn|=d.tick(in).spawn;assertTrue(spawn);
    }
    @Test public void approachRemovesAndDoesNotReturn(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.STALK,true);AmbientHooksDirector.Input in=input();
        in.figurePresent=true;in.distance=10;assertTrue(d.tick(in).remove);in.figurePresent=false;in.distance=100;
        for(int i=0;i<150;i++)assertFalse(d.tick(in).spawn);
    }
    @Test public void missingFigureFailsIntoAftermathAndCooldown(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.INTERCEPT,true);AmbientHooksDirector.Input in=input();
        for(int i=0;i<400;i++)d.tick(in);assertFalse(d.active());
    }
    @Test public void everyModeTerminatesEvenIfPlayerNeverResponds(){
        for(AmbientHooksDirector.Mode mode:AmbientHooksDirector.Mode.values()){
            AmbientHooksDirector d=new AmbientHooksDirector();d.begin(mode,false);AmbientHooksDirector.Input in=input();in.figurePresent=true;in.distance=25;in.visible=true;
            for(int i=0;i<1500;i++){AmbientHooksDirector.Action a=d.tick(in);if(a.remove)in.figurePresent=false;if(a.spawn)in.figurePresent=true;}
            assertFalse(mode.name(),d.active());
        }
    }
    @Test public void treelineDisappearsUnderScrutinyAndHasOnlyTwoReturns(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.TREE_LINE,true);AmbientHooksDirector.Input in=input();int spawns=0;
        for(int i=0;i<1000;i++){AmbientHooksDirector.Action a=d.tick(in);if(a.spawn){spawns++;in.figurePresent=true;in.observed=true;in.distance=26;}if(a.remove)in.figurePresent=false;}
        assertEquals(3,spawns);assertFalse(d.active());
    }
    @Test public void nullLurkNeverAdvancesAndDoesNotReturn(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.NULL_LURK,true);AmbientHooksDirector.Input in=input();int spawns=0;
        for(int i=0;i<600;i++){AmbientHooksDirector.Action a=d.tick(in);assertFalse(a.step);if(a.spawn){spawns++;in.figurePresent=true;in.distance=26;in.observed=true;}if(a.remove)in.figurePresent=false;}
        assertEquals(1,spawns);assertFalse(d.active());
    }
    @Test public void lostFootstepsRespondsToMovementThenTurning(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.LOST_FOOTSTEPS,true);AmbientHooksDirector.Input in=input();
        for(int i=0;i<35;i++){in.moving=true;d.tick(in);}in.turned=true;assertTrue(d.tick(in).spawn);
    }
    @Test public void repeaterHasFourCuesAndNoFigure(){
        AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.REPEATER,true);AmbientHooksDirector.Input in=input();int cues=0;
        for(int i=0;i<600;i++){AmbientHooksDirector.Action a=d.tick(in);if(a.cue)cues++;assertFalse(a.spawn);}
        assertEquals(4,cues);
    }
    @Test public void resetAbortsWithoutExtraActions(){AmbientHooksDirector d=new AmbientHooksDirector();d.begin(AmbientHooksDirector.Mode.TURN_AROUND,true);d.reset();AmbientHooksDirector.Action a=d.tick(input());assertFalse(d.active());assertFalse(a.spawn||a.cue||a.step);}
}
