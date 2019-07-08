import os
import subprocess
'''Script to run rdseed on all .seed files in a directory. 
Makes a subdirectory to hold all the produced SAC files.
To use, copy this into the directy holding the .seed files and run "python seedreader.py" 
on the terminal

<3 Zev 2019
'''

#execute rdseed on all .seedfiles in the current directory
seedList = os.popen('ls').read()
print seedList
for line in seedList.splitlines():
	if line.endswith('.seed'):
		os.system('rdseed -f ' +line +' -d')
os.system('ls')

#move all .SAC files to a subdirectory
os.system('mkdir SAClist')
filelist = os.popen('ls').read()
for line in filelist.splitlines():
	if line.endswith('.SAC'):
		os.system('mv '+line+' SAClist')
os.system('ls')


