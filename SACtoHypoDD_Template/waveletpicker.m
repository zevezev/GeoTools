function [ j ] = waveletpicker( y,N,threshold )
% This is an automatic p-wave picker, using the wavelet transform of the
% signal.
% input parameters: y=signal ; N=100 ; threshold=6
% output: index, j, of p-wave arrival time

[ct,~]=cwt(y,'amor');
ct2=real(ct);
ct2=sqrt(ct2.^2);

meancol=mean(ct2);
meancol=meancol-min(meancol);

for i=N+1:length(ct2)-N-1
    box1=mean(meancol(i-N:i));
    box2=mean(meancol(i:i+5));
    if box2/box1>=0.5*threshold
        lbox2=mean(meancol(i:i+N));
        if lbox2/box1>=threshold
            break;
        end
    end
end

for j=i:length(ct2)-5
    if abs(y(j+2))/abs(y(j))>=5
        break;
    end
end

end

