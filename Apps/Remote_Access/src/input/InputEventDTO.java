// input/InputEventDTO.java
package input;

import java.io.Serializable;

public class InputEventDTO implements Serializable {

    public static final int MOUSE_MOVE = 1;
    public static final int MOUSE_CLICK = 2;
    public static final int KEY = 3;
    public static final int DISCONNECT = 99;
    public static final int SCROLL = 4;

    public int type;
    public int x, y;
    public int button;
    public int keyCode;
    public int scrollAmount;
    public int clientWidth;
    public int clientHeight;
}
