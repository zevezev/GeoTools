clc; clear

stations=cell(21,1); %better data structure for this?
stations(1)={'TBCA'};
stations(2)={'TBCF'};
stations(3)={'TBHS'};
stations(4)={'TBHY'};
stations(5)={'TBMR'};
stations(6)={'TBTN'};
stations(7)={'BOA'};
stations(8)={'BOAB'};
stations(9)={'ACON'};
stations(10)={'BLUN'};
stations(11)={'CNGN'};
stations(12)={'CRIN'};
stations(13)={'CSGN'};
stations(14)={'ESPN'};
stations(15)={'ESTN'};
stations(16)={'HERN'};
stations(17)={'MASN'};
stations(18)={'MATN'};
stations(19)={'MGAN'};
stations(20)={'NANN'};
stations(21)={'RCON'};

disp(['    min ','max ',' std'])
for i=1:21
    
    fileID=fopen([char(stations(i)),'_test_output.txt'],'w');

    fnames=dir(['/Users/class/Desktop/geo165/*',char(stations(i)),'*BHZ*.SAC']);
    disp([string(length(fnames)), ' files for station ', char(stations(i))])
    
    datastore=cell(length(fnames),1);%read necessary files in for comparison, following indexing of fnames
    freqs=zeros(length(fnames),1);
    for j=1:length(fnames)
        datastore(j)={rsac([fnames(j).folder,'/',fnames(j).name])};
        freqs(j)=1/datastore{j}(1,3)/2; %read frequencies, why/2?
    end
    
    if length(freqs)>2
        disp([min(freqs),max(freqs),std(freqs)])
        %histogram(freqs)
    end
end

%     min   max    std
%     "0"    " files for station "    "TBCA"
% 
%     "0"    " files for station "    "TBCF"
% 
%     "790"    " files for station "    "TBHS"
% 
%     25    25     0
% 
%     "856"    " files for station "    "TBHY"
% 
%     25    25     0
% 
%     "933"    " files for station "    "TBMR"
% 
%     25    25     0
% 
%     "930"    " files for station "    "TBTN"
% 
%     25    25     0
% 
%     "68"    " files for station "    "BOA"
% 
%     10    10     0
% 
%     "68"    " files for station "    "BOAB"
% 
%     10    10     0
% 
%     "938"    " files for station "    "ACON"
% 
%     10    10     0
% 
%     "263"    " files for station "    "BLUN"
% 
%     10    10     0
% 
%     "920"    " files for station "    "CNGN"
% 
%     10    10     0
% 
%     "912"    " files for station "    "CRIN"
% 
%     10    10     0
% 
%     "565"    " files for station "    "CSGN"
% 
%     10    10     0
% 
%     "925"    " files for station "    "ESPN"
% 
%     10    10     0
% 
%     "4"    " files for station "    "ESTN"
% 
%     10    10     0
% 
%     "751"    " files for station "    "HERN"
% 
%     10    10     0
% 
%     "925"    " files for station "    "MASN"
% 
%     10    10     0
% 
%     "936"    " files for station "    "MATN"
% 
%     10    10     0
% 
%     "881"    " files for station "    "MGAN"
% 
%     10    10     0
% 
%     "762"    " files for station "    "NANN"
% 
%     10    10     0
% 
%     "0"    " files for station "    "RCON"