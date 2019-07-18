%code for plotting hypodd data in 3D

%[baseName, folder]= uigetfile('.loc', 'Pick a .loc Input File');
baseName = 'hypoDD.loc';
folder = '/Users/zisenberg/Documents/Lake_Managua_2014/SmallWindowSAC_BHZ';
fullFileName = fullfile(folder, baseName);
locdata = dlmread(fullFileName);

%[baseName2, folder2]= uigetfile('.reloc', 'Pick a .reloc Input File');
baseName2 = 'hypoDD.reloc';
folder2 = folder;
fullFileName2 = fullfile(folder2, baseName2);
relocdata = dlmread(fullFileName2);


scatter3(locdata(:,2),-locdata(:,3),-locdata(:,4)); hold on
scatter3(relocdata(:,2),-relocdata(:,3),-relocdata(:,4), 'filled'); hold off