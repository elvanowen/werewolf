import java.util.Scanner;

/**
 * Created by elvan_owen on 5/3/16.
 */
public class AsyncInput implements Runnable  {
    Thread t;
    String promptText;
    OnInputEnteredInterface onInputEnteredInterface;

    AsyncInput(String promptText){
        this.promptText = promptText;

        t = new Thread (this);
        t.start ();
    }

    public void run() {
        System.out.println(this.promptText);
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        while (!reader.hasNext()){}

        this.onInputEnteredInterface.onInputEntered(reader.next());
    }

    public void onInputEntered(OnInputEnteredInterface onInputEnteredInterface){
        this.onInputEnteredInterface = onInputEnteredInterface;
    }
}
