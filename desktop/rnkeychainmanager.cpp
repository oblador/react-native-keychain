/**
 * Copyright (c) 2017-present, Status Research and Development GmbH.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

#include "rnkeychainmanager.h"
#include "bridge.h"
#include "eventdispatcher.h"

#include <QDebug>
#include <QDir>
#include <QVariant>
#include <QByteArray>
#include <QEventLoop>

#include "keychain.h"
using namespace QKeychain;

static const QString keychainJobName("org.status.status-im.keychain");

namespace {
struct RegisterQMLMetaType {
    RegisterQMLMetaType() {
        qRegisterMetaType<RNKeychainManager*>();
    }
} registerMetaType;
} // namespace

class RNKeychainManagerPrivate {
public:
    Bridge* bridge = nullptr;
    QString username;
};

RNKeychainManager::RNKeychainManager(QObject* parent) : QObject(parent), d_ptr(new RNKeychainManagerPrivate) {}

RNKeychainManager::~RNKeychainManager() {}

void RNKeychainManager::setBridge(Bridge* bridge) {
    Q_D(RNKeychainManager);
    d->bridge = bridge;
}

QString RNKeychainManager::moduleName() {
    return "RNKeychainManager";
}

QList<ModuleMethod*> RNKeychainManager::methodsToExport() {
    return QList<ModuleMethod*>{};
}

QVariantMap RNKeychainManager::constantsToExport() {
    return QVariantMap();
}

void RNKeychainManager::getGenericPasswordForOptions(QVariantList options,
                                                     const ModuleInterface::ListArgumentBlock& resolve,
                                                     const ModuleInterface::ListArgumentBlock& reject) {
    Q_D(RNKeychainManager);
    qDebug()<<"invoked RNKeychainManager::getGenericPasswordForOptions";

    ReadPasswordJob rjob( keychainJobName );
    rjob.setAutoDelete( false );
    rjob.setKey( d->username );
    QEventLoop loop;
    rjob.connect( &rjob, SIGNAL(finished(QKeychain::Job*)), &loop, SLOT(quit()) );
    rjob.start();
    loop.exec();

    const QString pw = rjob.textData();
    if ( rjob.error() ) {
       qDebug() << "RNKeychainManager::getGenericPasswordForOptions failed: " << qPrintable(rjob.errorString());
       resolve(d->bridge, QVariantList{QVariant{false}});
       return;
    }
    qDebug() << "RNKeychainManager::getGenericPasswordForOptions success";
    resolve(d->bridge, QVariantList{QVariantMap{{"password", pw.toUtf8().data()}}});
}

void RNKeychainManager::setGenericPasswordForOptions(QVariantList options,
                                                     const QString &username,
                                                     const QString &password,
                                                     const QString& minSecLevel,
                                                     const ModuleInterface::ListArgumentBlock &resolve,
                                                     const ModuleInterface::ListArgumentBlock &reject) {
    Q_D(RNKeychainManager);
    qDebug()<<"invoked RNKeychainManager::setGenericPasswordForOptions";

    d->username = username;

    WritePasswordJob wjob( keychainJobName);
    wjob.setAutoDelete( false );
    wjob.setKey( username );
    wjob.setTextData( password );
    QEventLoop loop;
    wjob.connect( &wjob, SIGNAL(finished(QKeychain::Job*)), &loop, SLOT(quit()) );
    wjob.start();
    loop.exec();
    if ( wjob.error() ) {
        qDebug() << "RNKeychainManager::setGenericPasswordForOptions failed: " << qPrintable(wjob.errorString());
        reject(d->bridge, QVariantList{});
        return;
    }
    qDebug() << "RNKeychainManager::setGenericPasswordForOptions success";
    resolve(d->bridge, QVariantList{QVariant(true)});
}

void RNKeychainManager::resetGenericPasswordForOptions(QVariantList options,
                                                       const ModuleInterface::ListArgumentBlock &resolve,
                                                       const ModuleInterface::ListArgumentBlock &reject){
    Q_D(RNKeychainManager);
    qDebug()<<"invoked RNKeychainManager::resetGenericPasswordForOptions";

    d->username = "";

    DeletePasswordJob wjob( keychainJobName);
    wjob.setAutoDelete( false );
    QEventLoop loop;
    wjob.connect( &wjob, SIGNAL(finished(QKeychain::Job*)), &loop, SLOT(quit()) );
    wjob.start();
    loop.exec();
    if ( wjob.error() ) {
        qDebug() << "RNKeychainManager::resetGenericPasswordForOptions failed: " << qPrintable(wjob.errorString());
        reject(d->bridge, QVariantList{});
        return;
    }
    qDebug() << "RNKeychainManager::resetGenericPasswordForOptions success";
    resolve(d->bridge, QVariantList{});
}

void RNKeychainManager::setUsername(const QString &username,
                                    const ModuleInterface::ListArgumentBlock &resolve,
                                    const ModuleInterface::ListArgumentBlock &reject) {
    Q_D(RNKeychainManager);
    qDebug()<<"invoked RNKeychainManager::setUsername with username = " << username;
    d->username = username;

    resolve(d->bridge, QVariantList{QVariant(true)});
}
