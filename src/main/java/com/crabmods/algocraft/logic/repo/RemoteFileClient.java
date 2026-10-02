package com.crabmods.algocraft.logic.repo;

import java.io.IOException;
import java.net.URI;

/**
 * Fetches remote repository files. Kept injectable so repository sync can be
 * tested without live network access.
 */
interface RemoteFileClient {
    byte[] get(URI uri) throws IOException, InterruptedException;
}
