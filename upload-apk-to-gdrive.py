#!/usr/bin/python
import os
import subprocess
import sys

TW_APPS_FOLDER_NAME = "tw-onboarding-apps"

def getTwAppsFolderId():
    gdriveListCmd = 'gdrive list'
    grepFolderCmd = 'grep ' + TW_APPS_FOLDER_NAME
    cutFolderIdCmd = 'cut -d " " -f 1'

    p1 = subprocess.Popen(gdriveListCmd, shell=True, stdout=subprocess.PIPE)
    p2 = subprocess.Popen(grepFolderCmd, shell=True, stdin=p1.stdout, stdout=subprocess.PIPE)
    p1.stdout.close()
    p3 = subprocess.Popen(cutFolderIdCmd, shell=True, stdin=p2.stdout, stdout=subprocess.PIPE)
    p2.stdout.close()
    return p3.communicate()[0].strip()

def createTwAppsFolder():
    print "creating " + TW_APPS_FOLDER_NAME + " folder..."
    subprocess.call(['gdrive', 'mkdir', TW_APPS_FOLDER_NAME])
    return getTwAppsFolderId()

def apkPath():
    buildId = os.environ['GO_PIPELINE_COUNTER']
    apkName = 'tw-onboarding-release-' + buildId + '.apk'
    os.system('cp app/build/outputs/apk/release/*-release.apk ' + apkName)
    return apkName

def upload(folderId):
    path = apkPath()
    return os.system('gdrive upload -p ' + folderId + ' ' + path)

folderId = getTwAppsFolderId()

if not folderId:
    folderId = createTwAppsFolder()

result = upload(folderId)
sys.exit(result)