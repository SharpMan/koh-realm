package koh.realm.app;

import koh.patterns.services.api.Service;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Calendar;

/**
 *
 * @author Alleos13
 */

public class Logs implements Service {

    //private PrintStream Log_Errors;
    //private BufferedWriter Log_Infos;
    private final static Logger log = LoggerFactory.getLogger(Logs.class);
    private String createdDate;

    private boolean DEBUG = true;

    public Logs() {
        Reflections.log = null;
        this.createdDate = getDay();
    }

    private synchronized static void checkFolders(String createdDate) {
        if (!(new File("logs")).exists()) {
            new File("logs").mkdir();
        }
        if (!(new File("logs/" + createdDate)).exists()) {
            new File("logs/" + createdDate).mkdir();
        }
    }

    public synchronized void initialize() {
        this.close();
        this.createdDate = getDay();

        checkFolders(createdDate);
        /*try {
            Log_Errors = new PrintStream(new File("logs/" + createdDate + "/errors.log"));
        } catch (FileNotFoundException ex) {
        }

        System.setErr(Log_Errors);

        File fichier = new File("logs/" + createdDate + "/infos.log");
        try {
            FileWriter tmpWriter = new FileWriter(fichier, true);
            Log_Infos = new BufferedWriter(tmpWriter);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }*/
    }

    private static String getDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "-" + (Calendar.getInstance().get(+Calendar.MONTH) + 1) + "-" + Calendar.getInstance().get(Calendar.YEAR);
    }

    public synchronized void writeInfo(String toAdd) {
        log.info(toAdd);
        /*if (Log_Infos == null) {
            return;
        }

        if (!createdDate.equals(getDay())) {
            initialize();
        }

        Calendar target = Calendar.getInstance();
        String HOUR = Integer.toString(target.get(Calendar.HOUR_OF_DAY));
        HOUR = HOUR.length() == 1 ? ("0" + HOUR) : (HOUR);
        String MIN = Integer.toString(target.get(Calendar.MINUTE));
        MIN = MIN.length() == 1 ? ("0" + MIN) : (MIN);
        String SEC = Integer.toString(target.get(Calendar.SECOND));
        SEC = SEC.length() == 1 ? ("0" + SEC) : (SEC);

        String atTime = HOUR + ":" + MIN + ":" + SEC;
        try {
            //if(toWrite.size() ]= bufferSize)
            Log_Infos.write("[" + atTime + "] " + toAdd);
            Log_Infos.newLine();
            Log_Infos.flush();
        } catch (IOException ex) {
        }
        if (DEBUG) {
            System.out.println("[" + atTime + "] " + toAdd);
        }*/
    }

    public synchronized void writeDebug(String toAdd) {
        /*if (Log_Infos == null) {
            return;
        }

        if (!createdDate.equals(getDay())) {
            initialize();
        }

        Calendar target = Calendar.getInstance();
        String HOUR = Integer.toString(target.get(Calendar.HOUR_OF_DAY));
        HOUR = HOUR.length() == 1 ? ("0" + HOUR) : (HOUR);
        String MIN = Integer.toString(target.get(Calendar.MINUTE));
        MIN = MIN.length() == 1 ? ("0" + MIN) : (MIN);
        String SEC = Integer.toString(target.get(Calendar.SECOND));
        SEC = SEC.length() == 1 ? ("0" + SEC) : (SEC);

        String atTime = HOUR + ":" + MIN + ":" + SEC;
        if (DEBUG) {
            System.out.println("[" + atTime + "] " + toAdd);
        }*/

        log.debug(toAdd);
    }

    public synchronized void writeError(String toAdd) {
        /*if (Log_Errors == null) {
            return;
        }

        if (!createdDate.equals(getDay())) {
            initialize();
        }

        Calendar target = Calendar.getInstance();
        String HOUR = Integer.toString(target.get(Calendar.HOUR_OF_DAY));
        HOUR = HOUR.length() == 1 ? ("0" + HOUR) : (HOUR);
        String MIN = Integer.toString(target.get(Calendar.MINUTE));
        MIN = MIN.length() == 1 ? ("0" + MIN) : (MIN);
        String SEC = Integer.toString(target.get(Calendar.SECOND));
        SEC = SEC.length() == 1 ? ("0" + SEC) : (SEC);

        String atTime = HOUR + ":" + MIN + ":" + SEC;
        //if(toWrite.size() ]= bufferSize)
        Log_Errors.println("[" + atTime + "] " + toAdd);
        Log_Errors.flush();
        if (DEBUG) {
            System.out.println("[ERROR : " + atTime + "] " + toAdd);
        }*/

        log.error(toAdd);
    }

    public void close() {
        /*try {
            if (Log_Infos != null) {
                Log_Infos.flush();
                Log_Infos.close();
                Log_Infos = null;
            }
            if (Log_Errors != null) {
                Log_Errors.flush();
                Log_Errors.close();
                Log_Errors = null;
            }
        } catch (IOException e) {
        }*/
    }

    @Override
    public void start() {
        log.info("Logs starting");
        initialize();
    }

    @Override
    public void stop() {
        close();
    }

}
