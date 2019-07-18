''' plots output data from hypoDD. 
Blue dots are original locations, Red triangles are relocated locations

before running, install python and import matplotlib using pip if you have not yet. 
run the following on a terminal:
python -m pip install -U pip
python -m pip install -U matplotlib

to run, copy this file to the directory containing your .loc and .reloc files,
thenrun the following on a terminal:
python plotHypoDD.py

please email zev_izenberg@brown.edu with any questions or suggestoins

<3 Zev 2019
'''

# This import registers the 3D projection, but is otherwise unused.
from mpl_toolkits.mplot3d import Axes3D  # noqa: F401 unused import

import matplotlib.pyplot as plt


locfile = open('hypoDD.loc').read()
relocfile = open('hypoDD.reloc').read()

loclat = []
loclon = []
locdepth = []
for line in locfile.splitlines():
	columns = line.split()
	loclat.append(float(columns[1]))
	loclon.append(-float(columns[2]))
	locdepth.append(-float(columns[3])) 

reloclat = []
reloclon = []
relocdepth = []
for line in relocfile.splitlines():
	columns = line.split()
	reloclat.append(float(columns[1]))
	reloclon.append(-float(columns[2]))
	relocdepth.append(-float(columns[3])) 

fig = plt.figure()
ax = fig.add_subplot(111, projection='3d')

ax.scatter(loclat,loclon,locdepth, marker = 'o', s = 7, edgecolors = 'blue', facecolors = 'none', linewidths = .2)
ax.scatter(reloclat,reloclon,relocdepth, marker = '^', s = 5, c = 'red')

ax.set_xlabel('Latitude')
ax.set_ylabel('Longitude')
ax.set_zlabel('Depth (km)')

plt.show()
