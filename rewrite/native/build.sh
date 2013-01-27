#!/bin/sh

check_call()
{
	res=$?
	if [ $res -ne 0 ]
	then
		echo "Aborting"
		exit $res
	fi
}

echo "Choose build type:"
echo "  1) Win32 release (uses i686-w64-mingw32-gcc"
echo "  2) Linux64 (uses installed gcc)"
echo "Enter choice (1-2):"
read choice

#
# Win32rel
#
if [ "$choice" = "1" ]
then
	rm -rf build_Win32rel
	mkdir build_Win32rel
	cd build_Win32rel
	check_call

	#Check for cross compiler
	echo "Checking for i686-w64-mingw32-gcc"
	i686-w64-mingw32-gcc -v
	check_call

	cmake -DCMAKE_BUILD_TYPE=Release -DCRUXIC_CROSS_SYSTEM=Win32 -DCMAKE_TOOLCHAIN_FILE=toolchain-cross-i686-w64-mingw32.cmake ..
	check_call

	make

#
# Linux64
#
elif [ "$choice" = "2" ]
then
	rm -rf build_Linux64
	mkdir build_Linux64
	cd build_Linux64
	check_call

	cmake -DCMAKE_BUILD_TYPE=Release -DCRUXIC_CROSS_SYSTEM=Linux64 ..
	check_call

	make

else
	echo "Invalid choice"
	exit 1
fi
