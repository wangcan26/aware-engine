#!BPY

from __future__ import with_statement

""" Registration info for Blender menus: <- these words are ignored
Name: 'AwareEngine Scene Exporter'
Blender: 249
Group: 'Export'
Tip: 'Export scene info for use by the AwareEngine'
"""

__author__ = 'Cruxic'
__version__ = '0.1'
#__url__ = ["na", "na"]
#__email__=["cruxic", cruxic at gmail dot com"]


__bpydoc__ = """\
Export scene info for use by the AwareEngine
"""


import Blender
from Blender import *
import string

def main():

	# # Get the text
	# try:	cam_text = Blender.Text.Get('camera.py')
	# except:	cam_text = None
	#
	# if cam_text:
	# 	if cam_text.asLines()[0] != header:
	# 		ret = Blender.Draw.PupMenu("WARNING: An old camera.py exists%t|Overwrite|Rename old version text")
	# 		if ret == -1:			return # EXIT DO NOTHING
	# 		elif ret == 1:		Text.unlink(cam_text)
	# 		elif ret == 2:		cam_text.name = 'old_camera.txt'
	# 		cam_text = None
	#
	# if not cam_text:
	# 	scripting=Blender.Text.New('camera.py')
	# 	scripting.write(camera_change_scriptlink)

	scene = Scene.GetCurrent()
	numExported = 0
	exported = []
	for obj in scene.objects:
		if obj.getType() == 'Camera':
			name = obj.name

			#remove 'cam' prefix
			if name.startswith('cam '):
				name = name[len('cam '):]

			exported.append( (name, obj.LocX, obj.LocY, obj.LocZ) )

	#sort by name
	exported.sort(key=lambda rec: rec[0])

	with open('/home/cruxic/Programming/AwareEngine/other/out.txt', 'w+') as fout:
		for rec in exported:
			print >> fout, '%s\n\t%g,%g,%g' % rec
			numExported += 1

	Blender.Draw.PupMenu('Export Complete%t|exported ' + str(numExported))
	Blender.Window.RedrawAll()

if __name__ == '__main__':
	main()