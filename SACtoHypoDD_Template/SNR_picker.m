function [ ind,snr ] = SNR_picker( data,sp,ep,W1,W2 )
%finds the signal-to-noise ratio of a signal

for i=1:ep-sp+1
    noisewin = data(sp+i-W1:sp+i);
    sgnwin = data(sp+i:sp+i+W2);
    nstrength=rms(noisewin);
    sgstrength=max(sgnwin);
    %sgtstrength is the max value of the signal window
    %nstrength is the average(rms) value of the noise window
    snrstore(i) = sgstrength/nstrength;


end

ind = find(snrstore==max(snrstore))+sp;
%signal to noise ration becomes the max value of the ratio 
snr = max(snrstore);
