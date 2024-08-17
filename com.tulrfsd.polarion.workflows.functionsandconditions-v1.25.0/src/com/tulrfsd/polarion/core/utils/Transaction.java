package com.tulrfsd.polarion.core.utils;

import org.jetbrains.annotations.NotNull;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.RunnableInReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;

public interface Transaction {
  
  public static ReadOnlyTransaction getTransaction() {
    
    RunnableInReadOnlyTransaction<ReadOnlyTransaction> runnable = (@NotNull ReadOnlyTransaction transaction) -> transaction;
    return TransactionalExecutor.executeSafelyInReadOnlyTransaction(runnable);
  }

}
