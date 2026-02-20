package org.tn5250j.event;

import java.util.EventListener;

public interface BootListener extends EventListener {

    void bootOptionsReceived(BootEvent bootevent);

}
