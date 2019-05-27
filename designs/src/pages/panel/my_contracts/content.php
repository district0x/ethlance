<div class="content my-disputes-arbiter with-menu with-tabs">
    <?php include('../inc/lateral_menu.php'); ?>
    <div class="box">
        <div class="geral-buttons-opts-screen"> 
            <div class="flex">
                <a class="buttons-tabs-screen active"><span>Invitations</span></a>
                <a class="buttons-tabs-screen"><span>Pending Proposals</span></a>
                <a class="buttons-tabs-screen"><span>Active Contracts</span></a>
                <a class="buttons-tabs-screen"><span>Finished Contracts</span></a>
                <a class="buttons-tabs-screen"><span>Cancelled Contracts</span></a>
            </div>
            <div class="only-mobile combobox">
                <select>
                    <option value="0">Invitations</option>
                    <option value="1">Pending Proposals</option>
                    <option value="2">Active Contracts</option>
                    <option value="3">Finished Contracts</option>
                    <option value="4">Cancelled Contracts</option>
                </select>
                <div class="items"></div>
            </div>
            <span class="icon-opt-title"><img src="./images/svg/ic-white-arrow-down.svg" alt=""></span>
        </div>
        <div class="box-content box-shadow min-height">
            <?php include ('./table.php'); ?>
            <?php include ('./table.php'); ?>
            <?php include ('./table.php'); ?>
            <?php include ('./table.php'); ?>
            <?php include ('./table.php'); ?>
        </div>
    </div>
</div>