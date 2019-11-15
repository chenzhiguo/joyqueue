package io.chubao.joyqueue.nsr.nameservice;

import com.alibaba.fastjson.JSON;
import io.chubao.joyqueue.domain.AllMetadata;
import io.chubao.joyqueue.nsr.NameService;
import io.chubao.joyqueue.nsr.config.NameServiceConfig;
import io.chubao.joyqueue.toolkit.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NameServiceCompensateThread
 * author: gaohaoxiang
 * date: 2019/9/2
 */
public class NameServiceCompensateThread extends Service implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(NameServiceCompensateThread.class);

    private NameServiceConfig config;
    private NameService delegate;
    private NameServiceCacheManager nameServiceCacheManager;
    private NameServiceCompensator nameServiceCompensator;

    private Thread compensationThread;
    private volatile boolean started = false;

    public NameServiceCompensateThread(NameServiceConfig config, NameService delegate,
                                       NameServiceCacheManager nameServiceCacheManager, NameServiceCompensator nameServiceCompensator) {
        this.config = config;
        this.delegate = delegate;
        this.nameServiceCacheManager = nameServiceCacheManager;
        this.nameServiceCompensator = nameServiceCompensator;
    }

    @Override
    protected void validate() throws Exception {
        compensationThread = new Thread(this, "joyqueue-nameservice-compensation");
        compensationThread.setDaemon(true);
    }

    @Override
    protected void doStart() throws Exception {
        started = true;
        compensationThread.start();
    }

    @Override
    protected void doStop() {
        started = false;
    }

    @Override
    public void run() {
        while (started) {
            try {
                doCompensate();
            } catch (Exception e) {
                logger.error("compensate exception", e);
            }
            try {
                Thread.currentThread().sleep(config.getCompensationInterval());
            } catch (InterruptedException e) {
            }
        }
    }

    public void doCompensate() {
        NameServiceCache oldCache = nameServiceCacheManager.getCache();
        if (oldCache == null) {
            AllMetadata allMetadata = delegate.getAllMetadata();
            NameServiceCache newCache = nameServiceCacheManager.buildCache(allMetadata);

            if (logger.isDebugEnabled()) {
                logger.debug("doCompensate, newCache: {}, metadata: {}", JSON.toJSONString(newCache), JSON.toJSONString(allMetadata));
            }

            nameServiceCacheManager.fillCache(newCache);
        } else {
            if (nameServiceCacheManager.isLocked()) {
                return;
            }

            int oldVersion = oldCache.getVersion();

            if (logger.isDebugEnabled()) {
                logger.debug("doCompensate pre, oldCache: {}", JSON.toJSONString(oldCache));
            }

            AllMetadata allMetadata = delegate.getAllMetadata();
            NameServiceCache newCache = nameServiceCacheManager.buildCache(allMetadata);

            if (logger.isDebugEnabled()) {
                logger.debug("doCompensate, oldCache: {}, newCache: {}, metadata: {}",
                        JSON.toJSONString(oldCache), JSON.toJSONString(newCache), JSON.toJSONString(allMetadata));
            }

            if (!nameServiceCacheManager.isLocked() && oldCache.getVersion() == oldVersion) {
                if (config.getCompensationEnable()) {
                    nameServiceCompensator.compensate(oldCache, newCache);
                }
                nameServiceCacheManager.fillCache(newCache);
            }
        }
    }
}