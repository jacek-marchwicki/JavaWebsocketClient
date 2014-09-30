if [ -f "${HOME}/.ssh/id_rsa" ];
then
	echo "Never run this script on your computer - it is designed to run on build server"
	exit 1
fi

if [ ! -d "${HOME}/Android" ]; then
	mkdir -p "${HOME}/Android" || exit 1
fi

cd "${HOME}/Android" || exit 1
echo "Downloading sdk..."
wget --output-document=android-sdk.tgz --quiet http://dl.google.com/android/android-sdk_r23.0.2-linux.tgz || exit 1
tar xzf android-sdk.tgz > /dev/null || exit 1

export ANDROID_HOME="$HOME/Android/android-sdk-linux"
export PATH="${PATH}:${ANDROID_HOME}/platform-tools"
export PATH="${PATH}:${ANDROID_HOME}/tools/proguard/bin"
export PATH="${PATH}:${ANDROID_HOME}/tools"
export PATH="${PATH}:${ANDROID_NDK_HOME}"

RUN_TESTS=
#RUN_TESTS=yes

echo "Updating sdk..."
echo yes | android update sdk -a -u -t tools > /dev/null || exit 1
echo yes | android update sdk --filter platform-tools --no-ui --force > /dev/null || exit 1
echo yes | android update sdk -a -u -t build-tools-19.1.0 > /dev/null || exit 1

echo "Installing android..."
echo yes | android update sdk --filter android-19 --no-ui --force > /dev/null || exit 1
echo yes | android update sdk --filter addon-google_apis_x86-google-19 --no-ui --force > /dev/null || exit 1

echo "Installing support libraries..."
echo yes | android update sdk --filter extra-android-support --no-ui --force > /dev/null || exit 1
echo yes | android update sdk --filter extra-android-m2repository --no-ui --force > /dev/null || exit 1
echo yes | android update sdk --filter extra-google-m2repository --no-ui --force > /dev/null || exit 1

#echo "Displying current state..."
#android list targets
#android list sdk --no-ui --all --extended
#android list sdk
#find "$ANDROID_HOME/extras/android/m2repository"
#find "$ANDROID_HOME/extras/google/m2repository"

if [ ${RUN_TESTS} ];
then
	echo "Creating emulator..."
	echo "no" | android create avd \
		--device "Nexus 5" \
		--name test \
		--target "Google Inc.:Google APIs (x86 System Image):19" \
		--abi x86 \
		--skin WVGA800 \
		--sdcard 512M > /dev/null || exit 1
fi

echo "Preparing keys..."
mv /tmp/id_rsa "${HOME}/.ssh/id_rsa" || exit 1
mv /tmp/id_rsa.pub "${HOME}/.ssh/id_rsa.pub" || exit 1
chmod 0600 "${HOME}/.ssh/id_rsa" || exit 1

cat > "${HOME}/.ssh/known_hosts" <<EOF
[review.appunite.com]:29418 ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC7QO9BK5rR1/3draS4UZlfzRuCXsvauubbN3HNcJiFe4rqDNRTW5oKJ4US4wbds9lB58zrUW3e9mK7XiuX1VXcfZp18//kdozreTK2z37vC0l2KfScfn11EjwBsHztEm+ZTm8oir1ihHUsdob9dnMoMCvD1IpM1jwVsogNX8OwluUY4axlBv+meV4YGKN9YoFIEut9oM2eZXuDM7Yz9PxZ034vxtgMLKdIPfJDb11KmtdEkw5wPRBTq/2baYhacP3QZtXOHi2VWyMztmTsEg4Asl+eCSaXfQzNSQgJxWUxkvfX/bHz4/3DPaTsWESn76lwNQQVKKmp88LNQKUwHIEz
EOF

mkdir -p /tmp/workspace || exit 1
cd /tmp/workspace || exit 1
git init work || exit 1
cd work || exit 1
git fetch ssh://jenkins@$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT $GERRIT_REFSPEC && git checkout FETCH_HEAD || exit 1

cp /tmp/credentials.properties AndroidSocketIO/credentials.properties || exit 1

TASKS="build"

./gradlew --parallel \
	--refresh-dependencies ${TASKS} \
	--stacktrace \
	--project-prop versionSuffix="$GERRIT_CHANGE_NUMBER.$GERRIT_PATCHSET_NUMBER"

OUT=$?

if [ $OUT -eq 0 ];
then
	if [ ${RUN_TESTS} ];
	then
		echo "Waiting for hacks..."
		wait $CONNECT_PID
		HACKS_OUT=$?
		OUT=$HACKS_OUT

		if [ $HACKS_OUT -eq 0 ];
		then
			echo "Running tests..."

			# TODO: Run tests
			exit 1
			./gradlew --parallel --refresh-dependencies \
				:Yapert:connectedCheck \
				--stacktrace \
				--project-prop versionSuffix="$GERRIT_CHANGE_NUMBER.$GERRIT_PATCHSET_NUMBER"

			# Ignore tests output
			# OUT=$?
		else
			echo "Failed installing hacks"
		fi

	fi
fi

if [ $OUT -eq 0 ];
then
	if [ ${GERRIT_EVENT_TYPE} == "change-merged" ];
	then
		./gradlew uploadArchives \
			--project-prop versionSuffix="$GERRIT_CHANGE_NUMBER.$GERRIT_PATCHSET_NUMBER"
		OUT=$?
	fi
fi


mkdir -p /tmp/build/library
mv AndroidSocketIO/build/outputs /tmp/build/library/

mkdir -p /tmp/build/example
mv AndroidSocket/build/outputs /tmp/build/example/

exit $OUT
