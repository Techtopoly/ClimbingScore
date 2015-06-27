package rockscience.climbingscore;

/**
 * Created by Bentopia on 6/8/2015.
 */
public class Climb {

    private String _name, _score, _climbid, _start, _finish;
    private int _id;

    public Climb (int id, String name, String score, String climbid, String start, String finish) {
        _id = id;
        _name = name;
        _score = score;
        _climbid = climbid;
        _start = start;
        _finish = finish;
    }

    public int getId() { return _id; }

    public String getName() { return _name; }

    public String getScore() {
        return _score;
    }

    public String getClimbid() {
        return _climbid;
    }

    public String getStart() {
        return _start;
    }

    public String getFinish() { return _finish; }
}
