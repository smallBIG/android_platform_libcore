/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.net.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
// begin WITH_TAINT_TRACKING
import dalvik.system.Taint;
// end WITH_TAINT_TRACKING

/**
 * An HTTP request body that's completely buffered in memory. This allows
 * the post body to be transparently re-sent if the HTTP request must be
 * sent multiple times.
 */
final class RetryableOutputStream extends AbstractHttpOutputStream {
    private final int limit;
    private final ByteArrayOutputStream content;

    public RetryableOutputStream(int limit) {
        this.limit = limit;
        this.content = new ByteArrayOutputStream(limit);
    }

    public RetryableOutputStream() {
        this.limit = -1;
        this.content = new ByteArrayOutputStream();
    }

    @Override public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (content.size() < limit) {
            throw new IOException("content-length promised "
                    + limit + " bytes, but received " + content.size());
        }
    }

    @Override public synchronized void write(byte[] buffer, int offset, int count)
            throws IOException {
        checkNotClosed();
        Arrays.checkOffsetAndCount(buffer.length, offset, count);
        if (limit != -1 && content.size() > limit - count) {
            throw new IOException("exceeded content-length limit of " + limit + " bytes");
        }
				//begin  WITH_TAINT_TRACKING
				int taint = Taint.getTaintByteArray(buffer);
				if(taint != 0){
        	int disLen = count;
          if (count > Taint.dataBytesToLog) {
          	disLen = Taint.dataBytesToLog;
					}
          String dstr = new String(buffer, offset, disLen);
          // We only display at most Taint.dataBytesToLog characters in logcat
          // replace non-printable characters
          dstr = dstr.replaceAll("\\p{C}", ".");
          String tstr = "0x" + Integer.toHexString(taint);
          Taint.log("SESAME RetryableOutputStream#write " + dstr + " " + tstr);
				}
				//end    WITH_TAINT_TRACKING
        content.write(buffer, offset, count);
    }

    public synchronized int contentLength() throws IOException {
        close();
        return content.size();
    }

    public void writeToSocket(OutputStream socketOut) throws IOException  {
        content.writeTo(socketOut);
    }
}
