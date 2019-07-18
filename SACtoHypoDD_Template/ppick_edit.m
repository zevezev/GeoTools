function [ index ] = ppick_edit(y,N,threshold)
y=y./max(y);
index=1;
runningsum=sum(y(1:N-1));
runningsquare=sum(y(1:N-1).^2);
runningsqr2=sum(y(N-1:N-1+3).^2);
runningsqr3=sum(y(N-1:N-1+0.5*N).^2);

if sum(isnan(y))>0
    return
end

while index==1
    for i=N:length(y)-N
        if i==length(y)-N
            break;
        end
        runningsum=runningsum+y(i); %running average
        mean1=runningsum/i;
        mean2=y(i+2); %value of point 2
        runningsquare=runningsquare+y(i)^2;
        rms1=sqrt(runningsquare/i);%running RMS
        %rms2=rms(y(i:i+3)); %moving 3 point RMS (forward)
        runningsqr2=runningsqr2+y(i+3)^2-y(i-1)^2;
        rms2=sqrt(runningsqr2/4);
        %rms3=rms(y(i:i+0.5*N)); %moving N/2 point RMS
        runningsqr3=runningsqr3+y(i+0.5*N)^2-y(i-1)^2;
        rms3=sqrt(runningsqr3/(0.5*N+1)); 
        if abs(abs(mean2)-abs(mean1))>=threshold*rms1 && rms2/rms1>=threshold && rms3/rms1>=threshold
            index=i;
            break;
        end
    end
    threshold=threshold/2;
end