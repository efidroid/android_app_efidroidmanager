/*
 * Copyright (C) 2008 The Android Open Source Project
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
package org.efidroid.efidroidmanager.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.StringBuilderPrinter;

/**
 * IntentService is a base class for {@link Service}s that handle asynchronous
 * requests (expressed as {@link Intent}s) on demand.  Clients send requests
 * through {@link android.content.Context#startService(Intent)} calls; the
 * service is started as needed, handles each Intent in turn using a worker
 * thread, and stops itself when it runs out of work.
 * <p/>
 * <p>This "work queue processor" pattern is commonly used to offload tasks
 * from an application's main thread.  The IntentService class exists to
 * simplify this pattern and take care of the mechanics.  To use it, extend
 * IntentService and implement {@link #onHandleIntent(Intent)}.  IntentService
 * will receive the Intents, launch a worker thread, and stop the service as
 * appropriate.
 * <p/>
 * <p>All requests are handled on a single worker thread -- they may take as
 * long as necessary (and will not block the application's main loop), but
 * only one request will be processed at a time.
 * <p/>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For a detailed discussion about how to create services, read the
 * <a href="{@docRoot}guide/topics/fundamentals/services.html">Services</a> developer guide.</p>
 * </div>
 *
 * @see android.os.AsyncTask
 */
public abstract class IntentServiceEx extends Service {
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;
    private boolean mRedelivery;
    private static volatile boolean mShouldStop = false;

    private static final String ACTION_CLEAR_QUEUE = "internal_action_clear_queue";
    private static final String ACTION_STOP_CURRENT = "internal_action_stop_current";
    private static final String ACTION_SEND_MESSAGE = "internal_action_send_message";

    private static final String ARG_MESSAGE_INTENT = "internal_message_intent";

    public static void startActionClearQueue(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.setAction(ACTION_CLEAR_QUEUE);
        context.startService(intent);
    }

    public static void stopCurrentTask(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.setAction(ACTION_STOP_CURRENT);
        context.startService(intent);
    }

    public static void sendMessage(Context context, Class<?> cls, Intent intent) {
        Intent internal_intent = new Intent(context, cls);
        internal_intent.setAction(ACTION_SEND_MESSAGE);
        internal_intent.putExtra(ARG_MESSAGE_INTENT, intent);
        context.startService(internal_intent);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public IntentServiceEx(String name) {
        super();
        mName = name;
        mShouldStop = false;
    }

    /**
     * Sets intent redelivery preferences.  Usually called from the constructor
     * with your preferred semantics.
     * <p/>
     * <p>If enabled is true,
     * {@link #onStartCommand(Intent, int, int)} will return
     * {@link Service#START_REDELIVER_INTENT}, so if this process dies before
     * {@link #onHandleIntent(Intent)} returns, the process will be restarted
     * and the intent redelivered.  If multiple Intents have been sent, only
     * the most recent one is guaranteed to be redelivered.
     * <p/>
     * <p>If enabled is false (the default),
     * {@link #onStartCommand(Intent, int, int)} will return
     * {@link Service#START_NOT_STICKY}, and if the process dies, the Intent
     * dies along with it.
     */
    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        // TODO: It would be nice to have an option to hold a partial wakelock
        // during processing, and to have a static startService(Context, Intent)
        // method that would launch the service & hand off a wakelock.
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    protected void earlyStop() {

    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_CLEAR_QUEUE)) {
                clearQueue();
                return;
            }

            if (intent.getAction().equals(ACTION_STOP_CURRENT)) {
                mShouldStop = true;
                return;
            }

            if (intent.getAction().equals(ACTION_SEND_MESSAGE)) {
                Intent msgIntent = intent.getParcelableExtra(ARG_MESSAGE_INTENT);
                onHandleMessage(msgIntent);
                return;
            }
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    /**
     * You should not override this method for your IntentService. Instead,
     * override {@link #onHandleIntent}, which the system calls when the IntentService
     * receives a start request.
     *
     * @see android.app.Service#onStartCommand
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    /**
     * Unless you provide binding for your service, you don't need to implement this
     * method, because the default implementation returns null.
     *
     * @see android.app.Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);

    protected void onHandleMessage(Intent intent) {

    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (!shouldStop()) {
                onHandleIntent((Intent) msg.obj);
            } else {
                earlyStop();
            }

            mShouldStop = false;
            stopSelf(msg.arg1);
        }
    }

    public void clearQueue() {
        mServiceHandler.removeMessages(0);
    }

    public boolean shouldStop() {
        return mShouldStop;
    }

    public int queueSize() {
        StringBuilder sb = new StringBuilder();
        mServiceLooper.dump(new StringBuilderPrinter(sb), "");

        String[] lines = sb.toString().split("\n");
        return lines.length - 2;
    }
}