  if (window.top != window.self) {
    document.onclick = function(e) {
                        if (top.Jx.shell.sessionController.resetIdleNotifyTabs != null)
                            top.Jx.shell.sessionController.resetIdleNotifyTabs();
                     }

    document.onkeypress = function(e) {
                        if (top.Jx.shell.sessionController.resetIdleNotifyTabs != null)
                            top.Jx.shell.sessionController.resetIdleNotifyTabs();
                     }

    document.onmousemove = function(e) {
                        if (top.Jx.shell.sessionController.resetIdleNotifyTabs != null)
                            top.Jx.shell.sessionController.resetIdleNotifyTabs();
                     }
  }

