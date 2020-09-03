package rxhttp.wrapper.param;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

import okhttp3.Call;
import okhttp3.Response;
import rxhttp.IRxHttp;
import rxhttp.wrapper.callback.ProgressCallback;
import rxhttp.wrapper.entity.Progress;
import rxhttp.wrapper.entity.ProgressT;
import rxhttp.wrapper.param.FormParam;
import rxhttp.wrapper.param.RxHttpFormParam;
import rxhttp.wrapper.parse.DownloadParser;
import rxhttp.wrapper.parse.Parser;
import rxhttp.wrapper.utils.LogUtil;

/**
 * User: ljx
 * Date: 2018/04/20
 * Time: 11:15
 */
public final class ObservableCallExecute extends Observable<Progress> {

    private IRxHttp iRxHttp;
    private boolean callbackUploadProgress;

    public ObservableCallExecute(IRxHttp iRxHttp) {
        this(iRxHttp, false);
    }

    public ObservableCallExecute(IRxHttp iRxHttp, boolean callbackUploadProgress) {
        this.iRxHttp = iRxHttp;
        this.callbackUploadProgress = callbackUploadProgress;
    }

    @Override
    public void subscribeActual(Observer<? super Progress> observer) {
        HttpDisposable d = new HttpDisposable(observer, iRxHttp, callbackUploadProgress);
        observer.onSubscribe(d);
        if (d.isDisposed()) {
            return;
        }
        d.run();
    }

    private static class HttpDisposable implements Disposable, ProgressCallback {

        private boolean fusionMode;
        private volatile boolean disposed;

        private final Call call;
        private final Observer<? super Progress> downstream;

        /**
         * Constructs a DeferredScalarDisposable by wrapping the Observer.
         *
         * @param downstream the Observer to wrap, not null (not verified)
         */
        HttpDisposable(Observer<? super Progress> downstream, IRxHttp iRxHttp, boolean callbackUploadProgress) {
            if (iRxHttp instanceof RxHttpFormParam && callbackUploadProgress) {
                RxHttpFormParam formParam = (RxHttpFormParam) iRxHttp;
                FormParam param = formParam.getParam();
                param.setProgressCallback(this);
            }
            this.downstream = downstream;
            this.call = iRxHttp.newCall();
        }

        @Override
        public void onProgress(int progress, long currentSize, long totalSize) {
            if (!disposed) {
                Progress p = new Progress(progress, currentSize, totalSize);
                downstream.onNext(p);
            }
        }

        public void run() {
            Response value;
            try {
                value = call.execute();
            } catch (Throwable e) {
                LogUtil.log(call.request().url().toString(), e);
                Exceptions.throwIfFatal(e);
                if (!disposed) {
                    downstream.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }
            if (!disposed) {
                downstream.onNext(new ProgressT<>(value));
            }
            if (!disposed) {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            call.cancel();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }

    public <T> Observable<T> asParser(Parser<T> parser) {
        return asParser(parser, null, null);
    }

    public <T> Observable<T> asParser(Parser<T> parser, Consumer<Progress> progressConsumer) {
        return asParser(parser, progressConsumer, null);
    }

    public <T> Observable<T> asParser(Parser<T> parser, Consumer<Progress> progressConsumer, Scheduler scheduler) {
        return new ObservableParser<>(this, parser, progressConsumer, scheduler);
    }
}
