package scot.mclarentech.magicmirror;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextClock;

import androidx.annotation.RequiresApi;

public class MyTextClock extends TextClock {

    public MyTextClock(Context context) {
        super(context);
        //
        this.setDesigningText();
    }

    public MyTextClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        //
        this.setDesigningText();
    }

    public MyTextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //
        this.setDesigningText();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MyTextClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        //
        this.setDesigningText();
    }

    private void setDesigningText() {
        // The default text is displayed when designing the interface.
        this.setText("11:30:00");
    }

    //
    // Fix error: Exception raised during rendering.
    //
    // java.lang.NullPointerException
    //    at android.content.ContentResolver.registerContentObserver(ContentResolver.java:2263)
    //    at android.widget.TextClock.registerObserver(TextClock.java:626)
    //    at android.widget.TextClock.onAttachedToWindow(TextClock.java:545)
    //    at android.view.View.dispatchAttachedToWindow(View.java:19575)
    //    at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3437)
    //    at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3437)
    //    at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3437)
    //    at android.view.ViewGroup.dispatchAttachedToWindow(ViewGroup.java:3437)
    //    at android.view.AttachInfo_Accessor.setAttachInfo(AttachInfo_Accessor.java:42)
    //    at com.android.layoutlib.bridge.impl.RenderSessionImpl.inflate(RenderSessionImpl.java:335)
    //    at com.android.layoutlib.bridge.Bridge.createSession(Bridge.java:396)
    //    at com.android.tools.idea.layoutlib.LayoutLibrary.createSession(LayoutLibrary.java:209)
    //    at com.android.tools.idea.rendering.RenderTask.createRenderSession(RenderTask.java:608)
    //    at com.android.tools.idea.rendering.RenderTask.lambda$inflate$6(RenderTask.java:734)
    //    at java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1590)
    //    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
    //    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
    //    at java.lang.Thread.run(Thread.java:748)
    @Override
    protected void onAttachedToWindow() {
        try {
            super.onAttachedToWindow();
        } catch(Exception e)  {
        }
    }

}
