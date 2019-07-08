import os
'''removes all files from the current directory with a certain extension and/ or containing a certain phrase. 
If you don't want to remove based on extention or phrase, just leave the brackets empty. 
<3 Zev 2019'''

endingsToKeep = ['.BHZ.M.SAC', '.BHN.M.SAC', '.BHE.M.SAC','.HHZ.M.SAC','.HHN.M.SAC','.HHE.M.SAC', '.py']
phrasesToRemove = ['.SY.']

for file in os.popen('ls').read().splitlines():
	removed = False
	if endingsToKeep:
		goodEnding = False
		for ending in endingsToKeep:
			if file.endswith(ending):
				goodEnding= True
		if goodEnding is False:
			os.system('rm -f '+file)
			removed = True
	elif not removed:
		for phrase in phrasesToRemove:
			if phraseToRemove in file:
				os.system('rm -f '+file)

