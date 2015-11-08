package koh.realm.dao;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import koh.realm.entities.Account;
import koh.realm.utils.Util;

/**
 *
 * @author Alleos13
 */
public class AccountTicket {

    private String key;
    public Account account;
    private String ip;
    private Timer timer;

    public String getKey() {
        return key;
    }

    public String getIP() {
        return ip;
    }

    public AccountTicket(Account account, String ip) {
        this.account = account;
        this.ip = ip;
        this.key = Util.genTicketID().toString();
        //timer = createTimer();
        //timer.start();
    }

    private Timer createTimer() {
        ActionListener action = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                clear();
            }
        };

        return new Timer(5000, action);
    }

    public void clear() {
        //Main.gameServer.delWaitingCompte(this);
        this.key = null;
        this.account = null;
        this.ip = null;
        if (timer != null) {
            timer.stop();
        }
        this.timer = null;
    }

    public Account valid() {
        return account;
    }

    public boolean isValid() {
       // return timer != null && timer.isRunning();
        return true;
    }

    public boolean isCorrect(String GT_ip, String[] infos) {
        return isValid() && infos.length == 2
                && key.equals(infos[0]) && ip.equals(infos[1])
                && ip.equals(GT_ip);
    }
}
