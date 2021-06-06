package de.dytanic.cloudnet.ext.bridge.gomint.event;

import io.gomint.GoMint;
import io.gomint.event.Event;

/**
 * All CloudNet events will mostly fire asynchronously, depending on how they were triggered. Check {@link
 * GoMint#mainThread()} ()} and treat the event appropriately.
 */
abstract class GoMintBridgeEvent extends Event {

}
