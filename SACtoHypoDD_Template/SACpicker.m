clc; clear
%% 
%SACpickerreads info from .SAC files into pick lists for each station,
%and finds the cross-correlation times for each station too.
%%

%%log:
%EB 4/24/17
%added comments
%changed picker input to filt (y1b), from filtfilt (y1b_ff)
%changed most of the W1 references to be W1 from 2*W1
%changed frequency to rf (real frequency) and nf (nyquist frequency)
%added section to window +-2.5 around predicted arrival, and pick with envelope
%
% next edit: 
% Zev summer 2019
%Changed scriptname to SACpicker from sac2edit8
%Added comments to explain what code does
%simplified different areas
%renamed variables to make them understandable
%added README file
%added functionality to allow SAC on multiple frequency types (ex. BHZ,
%HHZ)
%

addpath('./Matlab_TauP/');
javaaddpath('./taup/lib/log4j-1.2.8.jar');
javaaddpath('./taup/lib/seisFile-1.0.1.jar');
javaaddpath('./taup/lib/TauP-1.1.7.jar');

functime=tic;

REM='myvmod.taup';

%% Constants
%TODO mess with the constants for different results, especially snrthre and
%the cross correlation window

%butterworth coefficients (in Hz)
ccFilterCoefficients=[0.2 5]; %ccFilter is filter for cross correlation
pickFilterCoefficients=[1 5]; %pickFilter is filter for picking
N=6; %padding for waveletpicker (need to check if this is ok on small series
n=2;
threshold=3;

%snrWindowWidths contains the width of the noise window first and the signal window width second
snrWindowWidths = [2,0.1];
%signal-to-noise threshhold - a higher threshold means a higher standard
SNRthreshold = 6;
%Window for picking
pickWindow=5; %half width
%window for cross correlation
ccWindowBeforeUnfiltered=1; %sec before the pick
ccWindowAfterUnfiltered=1; %sec after the pick
eventoffset=10; %sec

%TODO Make sure you fill out station.dat before running. May have to edit in textedit, or use nano on a terminal.
FID=fopen('./station.dat');
stations=textscan(FID,'%s %f %f');
fclose(FID);
stations=stations{1,1};

%test
stations = {'TBHS'};

%read in events requested from IRIS with location and magnitude data.
%TODO make sure events_OP.txt has the events you're looking for
eventTimes=containers.Map; 
eventlist=dlmread('events_OP.txt'); 
for i=1:length(eventlist)
    datevect=eventlist(i,1:6);
    eventTimes(datestr(datenum(double(datevect)),'yyyy mm dd HH MM SS'))=eventlist(i,:);
end

maplist=cell(length(stations),1);
snrmap=cell(length(stations),1);
snrstore=zeros(length(stations),1000);
%stores the names of stations that change frequencies.
problematicStations = cell(length(stations),1);
%TODO: change wavetypes to include only the frequency types you're looking
%at.
wavetypes = {'BHZ','HHZ'};
%%

for wavetype = 1:length(wavetypes)
    disp(wavetypes{1,wavetype});
    for i=1:length(stations)
        disp(num2str(i))
        dtfile=fopen([char(stations(i)),'_', wavetypes{1,wavetype},'_cc_output.txt'],'w'); %open files for writing to text
        ctfile=fopen([char(stations(i)),'_', wavetypes{1,wavetype},'_ppicks_output.txt'],'w');
        fprintf(ctfile,'Yr Mo Da Hr Mi Se stations{i} TT TTTau ttdiff ttndiff weight\r\n');
        logfile=fopen([char(stations(i)),'_', wavetypes{1,wavetype},'_log.txt'],'w');
        %TODO change this dir to the location of .SAC files, and choose signal
        %you're searching for - BHZ, HHZ, etc.
        %stationEvents is a list of the waveform recordings for the current station
        stationEvents=dir(['/Users/zisenberg/Documents/Lake_Managua_2014/SAClistFull/*'...
            ,char(stations(i)),'.*',wavetypes{1, wavetype},'*.M.SAC']); 
        disp([string(length(stationEvents)), ' files for station ', char(stations(i))])
        clear datastore stdstore atstore freq d snrstr
        pickTimes=containers.Map;
        stationSNRs=containers.Map;
        datastore=cell(length(stationEvents),1);%read necessary signals for comparison, follows indexing of fnames
        stdstore=cell(length(stationEvents),1); %store standard deviations
        freq=cell(length(stationEvents),1); %store frequencies
        atstore=cell(length(stationEvents),1); %store window's absolute index
        %samplerates stores the sampling rates foreach station; used to check
        %for stations that change samplingrates.
        samplerates = cell(length(stationEvents),1);
        for event=1:length(stationEvents)
            %sacData contains all of the event waveforms from each SAC file,
            %as well as info like samplerate.
            sacData=rsac([stationEvents(event).folder,'/',stationEvents(event).name]);
            %% Format data for picking and crosscorrelation
            samples = sacData(1,3);
            samplerates{event,1} = samples;
            nf=(1/samples)/2; %Nyquist frequency (samplingrate /2)
            samplerate=nf*2;
            pickWindowWidth=round(pickWindow*samplerate); % window width (# of samples)
            ccWindowBefore=round(ccWindowBeforeUnfiltered*samplerate);
            ccWindowAfter=round(ccWindowAfterUnfiltered*samplerate);

            filteredWaveform=double(detrend(sacData(:,2)));
            %figure(1);
            %plot(filteredWaveform);
            
            [b,a]=butter(4,ccFilterCoefficients./nf,'bandpass');
            [b2,a2]=butter(4,pickFilterCoefficients./nf,'bandpass');
            %waveformCC is the data filtered for cross correlation.
            %waveformPick is the data filtered for phase picking
            waveformCC=filtfilt(b,a,filteredWaveform); %two phase filter
            waveformPick=filter(b2,a2,filteredWaveform); %one phase filter
            %figure(2);
            %plot(waveformCC);

            % read in Lat, long
            stala=sacData(32,3);
            stalo=sacData(33,3);

            % read in waveform/sacfile start time, check if it's in the event list
            yr=sacData(71,3);
            [mo,dy]=jd2md(yr,sacData(72,3));
            hr=sacData(73,3);
            mn=sacData(74,3);
            se=sacData(75,3);
            ekey=datestr(datenum(double([yr mo dy hr mn se]))+eventoffset/84600,'yyyy mm dd HH MM SS');
            if ~isKey(eventTimes,ekey) %write out log of events not in event list c (should be 0)
                fprintf(logfile, [ekey, ' ', stations{i}]);
                continue;
            end

            % pull event info to local variables
            eveinfo=eventTimes(ekey);
            ela=eveinfo(7); 
            elo=eveinfo(8);
            edep=eveinfo(9);
            emag=eveinfo(10);
            dist = distance([stala stalo],[ela elo]);

            %% calculate theorhetical travel times
            import edu.sc.seis.TauP.*

            [~,taupout]=system(['TauP-2.4.3/bin/taup_time',...
                ' -mod ',REM,' -h ',num2str(edep),' -ph p,P -deg ',num2str(dist)]);

            if length(taupout)<300 %if text returned is too small, write to log
                fprintf(logfile, [ekey, ' ', stations{i}]);
                continue;
            end
            ttaup=str2num(taupout(295:300)); %extract travel time in seconds
            
            %TaupPickTime is the catalog phase pick.
            TaupPickTime=round((ttaup+eventoffset)*samplerate); % travel time + event offset = predicted arrival at station

            if TaupPickTime>=length(filteredWaveform) % log if arrival time is outside siesmogram length
                fprintf(logfile, [ekey, ' ', stations{i}]);
                continue;
            end

            if TaupPickTime+pickWindowWidth>=length(filteredWaveform) % if the arrival is too close to the end of the sacfile, set the index to 1/2 window
                TaupPickTime=length(filteredWaveform)-pickWindowWidth-1;
            end
            
            %pick_input applies a hilbert filter to our prefiltered
            %waveform
            pick_input=abs(hilbert(waveformPick));
            %figure(3);
            %plot(waveformPick);
            figure(4);
            plot(pick_input);

            %% This is the windowed picker.
            %remember windowPickTime is the time of the windowed pick, and
            %TaupPickTime is the time of the Taup pick.
            %snr is the signal-to-noise ratio
            [windowPickTime, snr] = SNR_picker(pick_input,TaupPickTime-pickWindowWidth,TaupPickTime+pickWindowWidth,snrWindowWidths(1)*samplerate,snrWindowWidths(2)*samplerate);
            %if the snr is too low, the data gets thrown out.
            if snr<SNRthreshold
                fprintf(logfile, [ekey, ' ', stations{i}]);
                continue;
            end
            hold off
            %figure(5);
            %plot(y1b_ff,'b');
            hold on
            %red dot is windowed pick, black is Taup - graph to check if
            %your window is working.
            %scatter(index1,0,'ro');
            %scatter(indext,0,'ko');
            hold off

            if windowPickTime<=ccWindowBefore % this should never happen
                windowPickTime=ccWindowBefore+1; 
            end
            if windowPickTime+ccWindowAfter/samplerate>=length(filteredWaveform)
                windowPickTime=windowPickTime-ccWindowAfter-1;
            end
            
            %% filter data for cross correlation using our new pick
            %y1c our data through the cross correlation window
            waveformCCWindowed=waveformCC((windowPickTime)-ccWindowBefore:(windowPickTime)+ccWindowAfter); %window for xcorr, offset by first window
            % taper
            hannw1=hann(length(waveformCCWindowed)); % Hanning tapering window
            %y1d is our data through the hanning window
            waveformCCFiltered=hannw1.*waveformCCWindowed;
            datastore(event)={waveformCCFiltered}; % filtfilt, windowed data
            %figure(6);
            %plot(datastore{j});
            atstore(event)={(windowPickTime)/(samplerate)+eventoffset}; % arrival time, offset by first window and event offset
            freq(event)={samplerate}; % frequency
            
            %% Output Windowed pick info to ppicks_output.txt
            Tevent=[sacData(71,3),0,sacData(72,3),sacData(73,3),sacData(74,3),sacData(75,3)+eventoffset+sacData(76,3)/1000]; % corrected event time [yr,m,d,h,min,sec]
            Tevent=datenum(double(Tevent)); % serial date number of event time
            Teventstr=datestr(Tevent,'yyyy mm dd HH MM SS');
            %TT is the time of windowed pick
            TT=(windowPickTime)/(samplerate)-eventoffset; % travel time minus the offset we used before the event
            %TTau is the time of the Taup pick
            TTTau=TaupPickTime/(samplerate)-eventoffset;
            %ttdiff is the difference between them - we're looking for
            %consistancy
            ttdiff=TTTau-TT;
            %not sure why we want ttndiff tbh
            ttndiff=ttdiff/TTTau;

            snrstore(i,event)=log10(snr);
            weight=1;
            if TT<=0
                weight=0;
            end
            %all of this info is printed into the station's
            %ppicks_output.txt file.
            fprintf(ctfile,'%6s %3s %3f %3f %3f %3f\r\n',Teventstr,stations{i},TT,TTTau,ttdiff,ttndiff, weight);

            if isKey(eventTimes,Teventstr) % this feeds travel times to phase.dat generation code at end
                pickTimes(Teventstr)=[stations{i},' ',num2str(TT)];
                stationSNRs(Teventstr)=snrstore(i,event);
            else
                fprintf(logfile, [Teventstr, ' ', stations{i}]); %write out log of events not in event list (should be 0)
            end

        end
        maplist(i)={pickTimes}; %only output outside of loop
        snrmap(i)={stationSNRs};


        %% perform crosscorrelation
        sta_start=tic; %start timer
        %compare every event from the station to every other event
        for waveform1=1:length(stationEvents)-1
            if isempty(datastore{waveform1}); continue; end
            %figure(7);
            %plot(datastore{k});
            for waveform2=waveform1+1:length(stationEvents)
                if isempty(datastore{waveform2}); continue; end
                %check the samplerates of k and m; if they are different, output the station name to
                %another cell array and skip the xcorr
                if samplerates{waveform1,1}~=samplerates{waveform2,1}
                    problematicStations(i) = stations(i);
                    continue;
                end;
                % xcorr  %we're interested in cross-covariance, rather than
                % cross correlation (don't care mean offsets since we're normalizing anyway)
                % see https://www.mathworks.com/matlabcentral/answers/23118-why-xcorr-coef-is-used-by-correlation-coefficients?s_tid=answers_rc1-1_p1_BOTH,
                % we want both unbiased (not changing with the number of points included) and normalized estimates

                [acorr,lag]=xcorr(datastore{waveform1},datastore{waveform2});
                %figure(8);
                %plot(datastore{k});
                %figure(9);
                %plot(datastore{m});
                
                %normalize the wavelengths by correlating with themselves,
                %then perform the crosscorrelation
                autoco1 = xcorr(datastore{waveform1},datastore{waveform1});
                autoco2 = xcorr(datastore{waveform2},datastore{waveform2});
                normcorr = ifft(fft(acorr)./sqrt(abs(fft(autoco1)).*abs(fft(autoco2)))); %normalize,and remove the bias
                [~,I] = max(abs(normcorr));
                %figure(10);
                %plot(lag,normcorr);

                if max(abs(normcorr))<-1
                    weight=0;
                else
                    weight=max(abs(normcorr)); 
                end

                if isnan(weight)
                    weight=0;
                end

                lagtime=lag(I)./(freq{waveform2})+atstore{waveform1}-atstore{waveform2};
                fprintf(dtfile,'%6s %6s %2f %2f\r\n',stationEvents(waveform1).name,stationEvents(waveform2).name,weight,lagtime);
            end
        end
        %if length(fnames)>0; histogram(corrs); pause; end
        if length(stationEvents)>2
            average_time=toc(sta_start)/nchoosek(length(stationEvents),2)
        end

    end

end

normsnr=max(max(snrstore));
%create phase.dat for ph2dt
%
%pull event info from original events file, 
%check for event in each travel time hash table (stored in cell), 
%write travel time if found
phasefile=fopen('phase.dat','w');
for o=1:length(eventlist)  
    fprintf(phasefile, '# %.4d %.2d %.2d %.2d %.2d %.2d %4f %4f %2f %2f %2f %2f %2f %.3d \r\n', eventlist(o,:));
    eventID=datestr(datenum(double(eventlist(o,1:6))),'yyyy mm dd HH MM SS');
    for p=1:length(stations)
        if not(isempty(maplist{p}))
            e=maplist{p};
            e1=snrmap{p};
            if isKey(e,eventID)
                snrnorm=e1(eventID)/normsnr;
                fprintf(phasefile, '%6s %.3f P\r\n', e(eventID),snrnorm);
            end
        end
    end
end

toc(functime)