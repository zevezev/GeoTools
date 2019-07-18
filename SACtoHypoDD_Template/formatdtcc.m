%% alters the dt.cc file to put all the cc picks into a format hypoDD can read.
%I'm augmenting the code to be able to take BHZ and HHZ files at once, even
%if they are in the same station.


clear all
FID=fopen('./station.dat');
stations=textscan(FID,'%s %f %f');
fclose(FID);
stations=stations{1,1};
%weightthre is the crosscorrelation strength threshhold - we want it as
% high as possible without removing too much data. 
weightthre = 0.7;
eventlist=dlmread('events_OP.txt'); 
for i=1:length(eventlist)
    Tevent=datenum([eventlist(i,1),eventlist(i,2),eventlist(i,3),eventlist(i,4)...
        ,eventlist(i,5),eventlist(i,6)-10]);
    Tstr=datestr(Tevent,'yyyy mm dd HH MM SS');
    yr=Tstr(1:4);
    mo=Tstr(6:7);
    dy=Tstr(9:10);
    hr=Tstr(12:13);
    mn=Tstr(15:16);
    se=Tstr(18:19);
    jd=floor(dayofyear(eventlist(i,1),eventlist(i,2),eventlist(i,3),...
        eventlist(i,4),eventlist(i,5),eventlist(i,6)-10));
    Events.(['EVE',yr,num2str(jd,'%.3d'),hr,mn,se])=num2str(eventlist(i,14),'%.3d');
end
pairs=[];
%put all the wavetypes you're using here, and augment all cc_output files
%in the directory to reflect their type. for example, change
%"TBCF_cc_output.txt to TBCF_cc_output.BHZ.txt
wavetypes = {'BHZ','HHZ'};
for wavetype = 1:length(wavetypes)
    disp(wavetype);
    %find the files for all wavetypes for all stations
    for is=1:length(stations)
        disp(num2str(is));
        STA=stations{is};
        filename = sprintf('./%s_%s_cc_output.txt',STA,wavetypes{1, wavetype});
        fid = fopen(filename);
        if fid ==-1, continue; end
        ccs=textscan(fid,'%s %s %f %f');
        fclose(fid);
        eve1s=ccs{1,1};
        if isempty(eve1s)
            continue;
        end
        eve2s=ccs{1,2};
        weights=ccs{1,3};
        lags=ccs{1,4};
        for ie=1:length(eve1s)
            eve1=eve1s{ie};
            eve1key=[eve1(1:4),eve1(6:8),eve1(10:11),eve1(13:14),eve1(16:17)];
            eve2=eve2s{ie};
            eve2key=[eve2(1:4),eve2(6:8),eve2(10:11),eve2(13:14),eve2(16:17)];
            if weights(ie) < weightthre
                weights(ie) = 0;
                continue;
            end
            if ~isfield(Events,['EVE',eve1key]) || ~isfield(Events,['EVE',eve2key])
                continue;
            end
            if ~isfield(pairs,['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])])
                pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).sta={STA};
                pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).weight=weights(ie);
                pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).lag=lags(ie);
            else
                pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).sta=...
                    [pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).sta;{STA}];
                pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).weight=...
                    [pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).weight;weights(ie)];
                pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).lag=...
                    [pairs.(['E',Events.(['EVE',eve1key]),Events.(['EVE',eve2key])]).lag;lags(ie)];
            end
        end
    end
end

pairname=fieldnames(pairs);
fdtcc=fopen('./dt.cc','w');
for ip=1:length(pairname)
    pname=pairname{ip};
    id1=pname(2:4);
    id2=pname(5:7);
    fprintf(fdtcc,'# %s %s 0\n',id1,id2);
    stas=pairs.(pname).sta;
    sweights=pairs.(pname).weight;
    slags=pairs.(pname).lag;
    for is=1:length(stas)
        fprintf(fdtcc,'%s %4f %4f P\n',stas{is},slags(is),sweights(is));       
    end
end
fclose(fdtcc);