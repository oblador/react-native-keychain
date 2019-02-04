/**
 * Copyright (c) 2017-present, Status Research and Development GmbH.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

#ifndef RNKEYCHAINMANAGER_H
#define RNKEYCHAINMANAGER_H

#include "moduleinterface.h"

#include <QVariantMap>
#include <QDateTime>

class RNKeychainManagerPrivate;
class RNKeychainManager : public QObject, public ModuleInterface {
    Q_OBJECT
    Q_INTERFACES(ModuleInterface)

    Q_DECLARE_PRIVATE(RNKeychainManager)

public:
    Q_INVOKABLE RNKeychainManager(QObject* parent = 0);
    ~RNKeychainManager();

    void setBridge(Bridge* bridge) override;

    QString moduleName() override;
    QList<ModuleMethod*> methodsToExport() override;
    QVariantMap constantsToExport() override;

    Q_INVOKABLE REACT_PROMISE void getGenericPasswordForOptions(QVariantList options,
                                                                const ModuleInterface::ListArgumentBlock& resolve,
                                                                const ModuleInterface::ListArgumentBlock& reject);
    Q_INVOKABLE REACT_PROMISE void setGenericPasswordForOptions(QVariantList options,
                                                                const QString& username,
                                                                const QString& password,
                                                                const QString& minSecLevel,
                                                                const ModuleInterface::ListArgumentBlock& resolve,
                                                                const ModuleInterface::ListArgumentBlock& reject);
    Q_INVOKABLE REACT_PROMISE void resetGenericPasswordForOptions(QVariantList options,
                                                                const ModuleInterface::ListArgumentBlock& resolve,
                                                                const ModuleInterface::ListArgumentBlock& reject);
    Q_INVOKABLE REACT_PROMISE void setUsername(const QString& username,
                                               const ModuleInterface::ListArgumentBlock& resolve,
                                               const ModuleInterface::ListArgumentBlock& reject);

private:
    QScopedPointer<RNKeychainManagerPrivate> d_ptr;
};

#endif // RNKEYCHAINMANAGER_H
