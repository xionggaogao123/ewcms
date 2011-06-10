/**
 * Copyright (c)2010-2011 Enterprise Website Content Management System(EWCMS), All rights reserved.
 * EWCMS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.ewcms.com
 */

package com.ewcms.scheduling.job.channel;

import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ewcms.core.site.model.Channel;
import com.ewcms.generator.GeneratorServiceable;
import com.ewcms.generator.release.ReleaseException;
import com.ewcms.scheduling.job.BaseEwcmsExecutionJob;
import com.ewcms.scheduling.job.channel.fac.EwcmsJobChannelFacable;
import com.ewcms.scheduling.job.channel.model.EwcmsJobChannel;

/**
 * 执行频道定时发布工作任务
 * 
 * @author 吴智俊
 */
public class EwcmsExecutionChannelJob extends BaseEwcmsExecutionJob {

    private static final Logger logger = LoggerFactory.getLogger(EwcmsExecutionChannelJob.class);
    
    private static final String SCHEDULER_FACTORY = "ewcmsJobChannelFac";
    private static final String PUBLISH_CHANNEL_FACTORY = "generatorService";

    protected EwcmsJobChannel jobDetails;
    
    protected void alqcJobExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            excutePublic();
        } catch (JobExecutionException e) {
        	logger.error("工作任务异常");
        	throw new JobExecutionException(e);
        } catch (SchedulerException e) {
        	logger.error("定时器异常");
            throw new JobExecutionException(e);
        } catch (Exception e) {
        	logger.error("发生异常");
        	throw new JobExecutionException(e);
        } finally {
            this.clear();
        }
    }

    protected void excutePublic() throws Exception {
        jobDetails = getJobDetails();
        Channel channel = jobDetails.getChannel();
        String channelName = "【" + channel.getName() + "】";
        
        Boolean subChannel = jobDetails.getSubChannel();
    	EwcmsJobChannelFacable ewcmsSchedulingFac = getEwcmsSchedulingFac();
    	EwcmsJobChannel jobChannel = ewcmsSchedulingFac.findJobChannelByChannelId(channel.getId());
    	if (jobChannel != null && jobChannel.getId().intValue() > 0){
			List<Channel> childens = ewcmsSchedulingFac.findChildenChannelByParentChannelId(channel.getId());
			if (subChannel && childens != null){
				subPublic(childens);
			}
    		if (channel.getPublicenable()){
				logger.info("定时发布 " + channelName + " 频道开始...");
				try{
					getGeneratorServiceable().generator(channel.getId());
				}catch(ReleaseException e){
					logger.error("定时发布 " + channelName + " 频道发布异常");
    			}
				logger.info("定时发布 " + channelName + " 频道结束.");
    		}
    	}
    }
       
    private void subPublic(List<Channel> channels) throws Exception {
    	EwcmsJobChannelFacable ewcmsSchedulingFac = getEwcmsSchedulingFac();
    	for (Channel channel : channels){
    		if (channel != null && channel.getId().intValue() > 0){
		    	List<Channel> childens = ewcmsSchedulingFac.findChildenChannelByParentChannelId(channel.getId());
		    	if (childens != null){
			    	subPublic(childens);
		    	}
    			if (channel.getPublicenable()){
			    	EwcmsJobChannel jobChannel = ewcmsSchedulingFac.findJobChannelByChannelId(channel.getId());
			    	if (jobChannel == null){
			    		String channelName = "【" + channel.getName() + "】";
			    		logger.info("定时发布 " + channelName + " 频道开始...");
						try{
							getGeneratorServiceable().generator(channel.getId());
						}catch(ReleaseException e){
							logger.error("定时发布 " + channelName + " 频道发布异常");
		    			}
			    		logger.info("定时发布 " + channelName + " 频道结束.");
			    	}
    			}
    		}
    	}
    }

    protected void alqcJobClear() {
        jobDetails = null;
    }

    protected EwcmsJobChannel getJobDetails() {
        EwcmsJobChannelFacable ewcmsSchedulingService = getEwcmsSchedulingFac();
        JobDataMap jobDataMap = jobContext.getTrigger().getJobDataMap();

        int jobId = jobDataMap.getInt(JOB_DATA_KEY_DETAILS_ID);
        EwcmsJobChannel jobChannel = ewcmsSchedulingService.getScheduledJobChannel(jobId);
        return jobChannel;
    }

    protected GeneratorServiceable getGeneratorServiceable(){
    	return (GeneratorServiceable) applicationContext.getBean(PUBLISH_CHANNEL_FACTORY);
    }
    
    protected EwcmsJobChannelFacable getEwcmsSchedulingFac() {
        return (EwcmsJobChannelFacable) applicationContext.getBean(SCHEDULER_FACTORY);
    }
}
